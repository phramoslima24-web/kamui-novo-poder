package com.kamuiprison.compat;

import com.kamuiprison.KamuiPrisonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Ponte com o sistema original do Narutoto.
 *
 * O Narutoto guarda o estado dos jutsus na capability
 * {@code NarutotoModVariables.PLAYER_VARIABLES_CAPABILITY} (campos publicos de {@code PlayerVariables}:
 * jutsu_choosed_name, jutsu_choosed, jutsu_keyN, chakra, learned_jutsu, jutsu_point, if_sharigan, ...).
 * Esta classe acessa exatamente esses mesmos campos por reflexao, entao o addon usa o mesmo estado,
 * a mesma sincronizacao ({@code syncPlayerVariables}) e o mesmo overlay do mod.
 */
public final class NarutoBridge {
    public static final String NARUTOTO_ID = "narutoto";

    private static final String VARIABLES_CLASS = "net.mcreator.narutoto.network.NarutotoModVariables";
    private static final String PLAYER_VARIABLES_CLASS = VARIABLES_CLASS + "$PlayerVariables";

    private static final Map<String, Field> FIELDS = new HashMap<>();
    private static boolean initialized;
    private static Capability<?> capability;
    private static Class<?> playerVariablesClass;
    private static Method syncMethod;

    private NarutoBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(NARUTOTO_ID);
    }

    private static synchronized boolean init() {
        if (initialized) {
            return capability != null;
        }
        initialized = true;
        try {
            Class<?> variables = Class.forName(VARIABLES_CLASS);
            capability = (Capability<?>) variables.getField("PLAYER_VARIABLES_CAPABILITY").get(null);
            playerVariablesClass = Class.forName(PLAYER_VARIABLES_CLASS);
            syncMethod = playerVariablesClass.getMethod("syncPlayerVariables", Entity.class);
        } catch (Throwable t) {
            capability = null;
            KamuiPrisonMod.LOGGER.error("Nao foi possivel acessar o sistema de variaveis do Narutoto. "
                    + "O Narutoto 0.4.1 esta instalado?", t);
        }
        return capability != null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object lookup(Entity entity) {
        if (entity == null || !init()) {
            return null;
        }
        LazyOptional lazy = entity.getCapability((Capability) capability);
        return lazy.orElse(null);
    }

    private static synchronized Field field(String name) {
        Field cached = FIELDS.get(name);
        if (cached != null) {
            return cached;
        }
        try {
            Field f = playerVariablesClass.getField(name);
            f.setAccessible(true);
            FIELDS.put(name, f);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Retorna o acesso as PlayerVariables da entidade, ou null se indisponivel. */
    public static Vars of(Entity entity) {
        Object target = lookup(entity);
        return target == null ? null : new Vars(entity, target);
    }

    public static boolean isInDimension(Entity entity, String dimensionId) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        return entity.level().dimension() == key;
    }

    /** Acesso (leitura/escrita) aos campos publicos de PlayerVariables. */
    public static final class Vars {
        private final Entity owner;
        private final Object target;

        private Vars(Entity owner, Object target) {
            this.owner = owner;
            this.target = target;
        }

        public boolean has(String name) {
            return field(name) != null;
        }

        public String getString(String name) {
            Field f = field(name);
            if (f == null) {
                return "";
            }
            try {
                Object v = f.get(target);
                return v == null ? "" : v.toString();
            } catch (Throwable t) {
                return "";
            }
        }

        public double getDouble(String name) {
            Field f = field(name);
            if (f == null) {
                return 0.0;
            }
            try {
                Object v = f.get(target);
                return v instanceof Number n ? n.doubleValue() : 0.0;
            } catch (Throwable t) {
                return 0.0;
            }
        }

        public void setString(String name, String value) {
            Field f = field(name);
            if (f == null) {
                return;
            }
            try {
                f.set(target, value);
            } catch (Throwable t) {
                KamuiPrisonMod.LOGGER.warn("Falha ao escrever {}", name, t);
            }
        }

        public void setDouble(String name, double value) {
            Field f = field(name);
            if (f == null) {
                return;
            }
            try {
                f.set(target, value);
            } catch (Throwable t) {
                KamuiPrisonMod.LOGGER.warn("Falha ao escrever {}", name, t);
            }
        }

        /** Sincroniza com o cliente usando o mesmo metodo do Narutoto (somente no servidor). */
        public void sync() {
            if (owner.level().isClientSide()) {
                return;
            }
            try {
                syncMethod.invoke(target, owner);
            } catch (Throwable t) {
                KamuiPrisonMod.LOGGER.warn("Falha ao sincronizar as variaveis do Narutoto", t);
            }
        }
    }
}
