package fr.insalyon.creatis.gasw;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class GaswLauncher {

    private static AnnotationConfigApplicationContext context;

    public static Gasw start() throws GaswException {
        context = new AnnotationConfigApplicationContext(GaswSpringConfig.class);
        return context.getBean(Gasw.class);
    }

    public static void stop(boolean force) throws GaswException {
        if (context != null) {
            context.getBean(Gasw.class).terminate(force);
        }
    }
}