package com.kamuiprison.client;

import com.kamuiprison.network.ModNetwork;
import com.kamuiprison.network.PrisonerView;
import com.kamuiprison.network.ReleasePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/** Interface do modo Parcial: um botao por prisioneiro (mobs e jogadores). Clicar liberta no local do usuario. */
@OnlyIn(Dist.CLIENT)
public class KamuiPrisonScreen extends Screen {
    private static final int PER_PAGE = 7;
    private static final int ROW_WIDTH = 240;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;

    private List<PrisonerView> prisoners;
    private int page;

    public KamuiPrisonScreen(List<PrisonerView> prisoners) {
        super(Component.translatable("gui.kamuiprison.prison.title"));
        this.prisoners = new ArrayList<>(prisoners);
    }

    /** Atualiza a lista (apos uma libertacao) mantendo a pagina atual quando possivel. */
    public void setPrisoners(List<PrisonerView> updated) {
        this.prisoners = new ArrayList<>(updated);
        int pages = pageCount();
        if (page >= pages) {
            page = pages - 1;
        }
        if (page < 0) {
            page = 0;
        }
        this.rebuildWidgets();
    }

    private int pageCount() {
        return Math.max(1, (prisoners.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private static Component label(PrisonerView p) {
        if (p.player()) {
            return Component.translatable("gui.kamuiprison.prison.entry_player", p.name());
        }
        if (p.typeName().isEmpty() || p.typeName().equals(p.name())) {
            return Component.translatable("gui.kamuiprison.prison.entry_mob", p.name());
        }
        return Component.translatable("gui.kamuiprison.prison.entry_mob_named", p.name(), p.typeName());
    }

    @Override
    protected void init() {
        int left = (this.width - ROW_WIDTH) / 2;
        int top = this.height / 2 - 85;

        int start = page * PER_PAGE;
        int end = Math.min(prisoners.size(), start + PER_PAGE);
        for (int i = start; i < end; i++) {
            PrisonerView prisoner = prisoners.get(i);
            int y = top + 24 + (i - start) * (ROW_HEIGHT + ROW_GAP);
            this.addRenderableWidget(Button.builder(label(prisoner),
                            b -> ModNetwork.CHANNEL.sendToServer(new ReleasePacket(prisoner.id())))
                    .bounds(left, y, ROW_WIDTH, ROW_HEIGHT)
                    .build());
        }

        int navY = top + 24 + PER_PAGE * (ROW_HEIGHT + ROW_GAP) + 4;
        Button prev = Button.builder(Component.translatable("gui.kamuiprison.prison.prev"), b -> {
            page--;
            this.rebuildWidgets();
        }).bounds(left, navY, 70, 20).build();
        prev.active = page > 0;
        this.addRenderableWidget(prev);

        Button next = Button.builder(Component.translatable("gui.kamuiprison.prison.next"), b -> {
            page++;
            this.rebuildWidgets();
        }).bounds(left + ROW_WIDTH - 70, navY, 70, 20).build();
        next.active = page < pageCount() - 1;
        this.addRenderableWidget(next);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kamuiprison.prison.close"),
                b -> this.onClose()).bounds(left + 75, navY, ROW_WIDTH - 150, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int top = this.height / 2 - 85;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top, 0xFFFFFF);
        if (prisoners.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.kamuiprison.prison.empty"),
                    this.width / 2, top + 40, 0xAAAAAA);
        } else {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.kamuiprison.prison.page", page + 1, pageCount()),
                    this.width / 2, top + 12, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
