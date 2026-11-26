package com.kamuiprison.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Compatibilidade OPCIONAL com "Narutoto - Mangekyou Addon" (modId {@value #MOD_ID}).
 *
 * <h3>Por que esta classe existe</h3>
 * O Mangekyou Addon registra entidades temporarias ligadas ao dono (Susanoo de Itachi,
 * Shisui e Madara, corpo do Izanagi, Espelho de Yata, lanca e projeteis do Susanoo).
 * O Susanoo ({@code CharacterSusanoo}) estende {@code TamableAnimal}, ou seja, e um
 * {@code Mob}: o Kamui Prison (Total) o aceitaria como alvo, serializaria e descartaria
 * a entidade, e a liberacao criaria uma copia fora do {@code SusanooManager} do addon
 * (Susanoo duplicado / estado do dono corrompido).
 *
 * <p>Estas entidades nao fazem sentido como prisioneiras, entao o Kamui Prison as trata
 * como "nao seladas" (mesma mensagem {@code message.kamuiprison.cannot_seal} ja usada
 * para alvos nao serializaveis).
 *
 * <h3>Garantias</h3>
 * <ul>
 *   <li>Nenhuma classe do addon e referenciada: a checagem e feita pelo NAMESPACE do
 *       registro da entidade, portanto e segura (e sem custo relevante) sem o addon.</li>
 *   <li>Nao altera nenhum comportamento de entidades do Narutoto ou de outros mods.</li>
 * </ul>
 */
public final class MangekyouAddonCompat {

    public static final String MOD_ID = "narutoto_mangekyou";

    private MangekyouAddonCompat() {
    }

    /** A entidade pertence ao Mangekyou Addon e nao pode ser enviada para a prisao? */
    public static boolean isProtectedFromSealing(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && MOD_ID.equals(key.getNamespace());
    }
}
