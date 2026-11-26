package com.kamuiprison.compat;

import net.minecraftforge.fml.ModList;

/**
 * Compatibilidade OPCIONAL com "Narutoto: Obito Powers Unlocked" (modId {@value #MOD_ID}).
 *
 * <h3>Por que esta classe existe</h3>
 * O Obito Unlocked altera o Kamui original do Narutoto 0.4.1 com cinco Mixins
 * ({@code @Redirect} em {@code GETFIELD}) aplicados nos procedures
 * {@code KamuiUserProcedure}, {@code KamuiRightClickProcedure},
 * {@code KamuiIntangibleJutsuProcedure}, {@code KamuiTransferJutsuProcedure} e
 * {@code KamuiTransferJutsu2Procedure}:
 * <ul>
 *   <li>{@code PlayerVariables.if_sharigan} passa a ser lido como {@code 2.0}
 *       quando {@code mangekyo_owner} e "sasuke" (ignorando maiusculas);</li>
 *   <li>(so no {@code KamuiRightClickProcedure}) {@code PlayerVariables.mangekyo_owner}
 *       passa a ser lido como "obito" quando o valor real e "sasuke".</li>
 * </ul>
 * Esses redirects so enxergam o codigo do proprio Narutoto. O Kamui Prison le as
 * variaveis por REFLEXAO ({@link NarutoBridge}), entao ve sempre o valor real e
 * precisa aplicar a mesma regra por conta propria — caso contrario um jogador
 * "sasuke" usa o Kamui do Obito, mas nao consegue aprender/usar a Prisao.
 *
 * <h3>Garantias</h3>
 * <ul>
 *   <li>Nao referencia nenhuma classe do addon (deteccao somente via {@link ModList}).</li>
 *   <li>Nao cria Mixins e nao toca nos Mixins do addon.</li>
 *   <li>Sem o addon instalado, o comportamento e IDENTICO ao do Kamui Prison 1.0.0
 *       com o Narutoto 0.4.1 puro.</li>
 * </ul>
 */
public final class ObitoUnlockedCompat {

    public static final String MOD_ID = "narutoto_obito_unlocked";

    /** if_sharigan do Narutoto: 2 = Mangekyo (caminho Obito). */
    private static final double STAGE_MANGEKYO = 2.0;

    private static final String OWNER_OBITO = "obito";
    private static final String OWNER_SASUKE = "sasuke";

    /** null = ainda nao consultado (ModList pode nao estar pronto muito cedo no ciclo de vida). */
    private static volatile Boolean loaded;

    private ObitoUnlockedCompat() {
    }

    /** O Obito Unlocked esta instalado? (somente consulta ao ModList; seguro sem o addon). */
    public static boolean isLoaded() {
        Boolean cached = loaded;
        if (cached == null) {
            ModList list = ModList.get();
            if (list == null) {
                return false; // nao cacheia: tenta de novo quando o ModList existir
            }
            cached = list.isLoaded(MOD_ID);
            loaded = cached;
        }
        return cached;
    }

    private static boolean isSasuke(String owner) {
        return OWNER_SASUKE.equalsIgnoreCase(owner);
    }

    /**
     * O dono do Mangekyo recebe os poderes de Kamui do Obito?
     * <ul>
     *   <li>"obito": sempre (regra original);</li>
     *   <li>"sasuke": somente se o Obito Unlocked estiver instalado.</li>
     * </ul>
     * Usado na condicao de aprendizado ({@code requireObitoMangekyo}).
     */
    public static boolean hasObitoKamui(String mangekyoOwner) {
        if (mangekyoOwner == null) {
            return false;
        }
        return OWNER_OBITO.equalsIgnoreCase(mangekyoOwner) || (isSasuke(mangekyoOwner) && isLoaded());
    }

    /**
     * O Narutoto considera o Mangekyo apto para usar Kamui?
     *
     * Os Mixins reais do Obito Unlocked redirecionam a leitura de
     * PlayerVariables.if_sharigan para 2.0 sempre que mangekyo_owner == "sasuke".
     * Portanto, com o addon instalado, o caminho do Sasuke segue a mesma condição
     * efetiva do Narutoto modificado, independentemente do valor real do estágio.
     * Para os demais donos, preservamos a condição original if_sharigan == 2.0.
     */
    public static boolean isMangekyoOpenForKamui(NarutoBridge.Vars vars) {
        if (vars == null) {
            return false;
        }
        String owner = vars.getString("mangekyo_owner");
        if (isSasuke(owner) && isLoaded()) {
            return true;
        }
        return vars.getDouble("if_sharigan") == STAGE_MANGEKYO;
    }
}
