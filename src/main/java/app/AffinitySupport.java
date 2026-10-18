package app;

import java.lang.reflect.Method;

public final class AffinitySupport {

    private AffinitySupport() { }

    public static void bind(int idCpu) {
        try {
            Class<?> classeAfinidade = Class.forName("net.openhft.affinity.Affinity");
            Method metodoSetAffinity = classeAfinidade.getMethod("setAffinity", int.class);
            metodoSetAffinity.invoke(null, idCpu);
        } catch (ClassNotFoundException e) {
            // Biblioteca OpenHFT opcional
        } catch (Exception ignored) {
        }
    }
}