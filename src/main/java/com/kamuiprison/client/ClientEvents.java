package com.kamuiprison.client;

import com.kamuiprison.KamuiPrisonMod;
import com.kamuiprison.jutsu.KamuiPrisonJutsu;
import com.kamuiprison.network.LearnPacket;
import com.kamuiprison.network.ModNetwork;
import com.kamuiprison.network.SwitchModePacket;
import com.kamuiprison.network.UsePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

/**
 * Integracao no cliente com o Narutoto:
 *  - Reaproveita as MESMAS teclas do mod original (setas para alternar o modo, tecla de usar o jutsu)
 *    lendo os KeyMapping dele; se nao encontrar, usa as teclas padrao (setas e Z).
 *  - Acrescenta o botao "Kamui: Prisao" na propria tela Dojutsu do Narutoto.
 */
@Mod.EventBusSubscriber(modid = KamuiPrisonMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final String KEYS_CLASS = "net.mcreator.narutoto.init.NarutotoModKeyMappings";
    private static final String DOJUTSU_SCREEN = "net.mcreator.narutoto.client.gui.DojutsuBoardScreen";

    private static boolean keysResolved;
    private static KeyMapping switchUp;
    private static KeyMapping switchDown;
    private static KeyMapping useKey;

    private ClientEvents() {
    }

    private static KeyMapping readKey(Class<?> holder, String field) {
        try {
            Field f = holder.getField(field);
            Object value = f.get(null);
            return value instanceof KeyMapping key ? key : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized void resolveKeys() {
        if (keysResolved) {
            return;
        }
        keysResolved = true;
        try {
            Class<?> holder = Class.forName(KEYS_CLASS);
            switchUp = readKey(holder, "SWITCH_TYPE_UPK");
            switchDown = readKey(holder, "SWITCH_TYPE_DOWN_K");
            useKey = readKey(holder, "RELEASE_JUSTU");
        } catch (Throwable t) {
            KamuiPrisonMod.LOGGER.warn("KeyMappings do Narutoto nao encontrados; usando teclas padrao.");
        }
    }

    private static boolean matches(KeyMapping mapping, int key, int scanCode, int defaultKey) {
        return mapping != null ? mapping.matches(key, scanCode) : key == defaultKey;
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        resolveKeys();
        int key = event.getKey();
        int scan = event.getScanCode();

        if (event.getAction() == GLFW.GLFW_PRESS) {
            boolean arrow = matches(switchUp, key, scan, GLFW.GLFW_KEY_RIGHT)
                    || matches(switchDown, key, scan, GLFW.GLFW_KEY_LEFT);
            if (arrow && KamuiPrisonJutsu.isSelected(mc.player)) {
                ModNetwork.CHANNEL.sendToServer(new SwitchModePacket());
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (matches(useKey, key, scan, GLFW.GLFW_KEY_Z) && KamuiPrisonJutsu.isPartialSelected(mc.player)) {
                ModNetwork.CHANNEL.sendToServer(new UsePacket());
            }
        }
    }

    /** Botao do jutsu dentro da tela Dojutsu original (abaixo do painel, alinhado as duas colunas). */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!DOJUTSU_SCREEN.equals(screen.getClass().getName())
                || !(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        Button button = Button.builder(Component.translatable("gui.kamuiprison.dojutsu_board.button"),
                        b -> ModNetwork.CHANNEL.sendToServer(new LearnPacket()))
                .bounds(container.getGuiLeft() + 28, container.getGuiTop() + 180, 157, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.kamuiprison.dojutsu_board.tooltip")))
                .build();
        event.addListener(button);
    }
}
