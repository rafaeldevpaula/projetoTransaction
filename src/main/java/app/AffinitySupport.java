package app;

import java.lang.reflect.Method;

final class AffinitySupport {
    private AffinitySupport() { }

    static void bind(int cpu) {
        try {
            Class<?> affinity = Class.forName("net.openhft.affinity.Affinity");
            Method setAffinity = affinity.getMethod("setAffinity", int.class);
            setAffinity.invoke(null, cpu);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Afinidade e' opcional: a analise continua valida sem suporte nativo.
        }
    }
}