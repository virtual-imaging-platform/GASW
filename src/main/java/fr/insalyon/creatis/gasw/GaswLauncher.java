package fr.insalyon.creatis.gasw;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.File;
import java.io.IOException;

public class GaswLauncher {

    private static AnnotationConfigApplicationContext context;

    public static Gasw start() throws GaswException {
        context = new AnnotationConfigApplicationContext();
        context.register(GaswSpringConfig.class);
        loadExternalConfig(context.getEnvironment());
        context.refresh();
        return context.getBean(Gasw.class);
    }

    public static void stop(boolean force) throws GaswException {
        if (context != null) {
            context.getBean(Gasw.class).terminate(force);
        }
    }

    private static void loadExternalConfig(ConfigurableEnvironment env) throws GaswException {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".gasw");
        if (!dir.exists()) return;

        File[] files = dir.listFiles((d, n) -> n.endsWith(".properties"));
        if (files == null) return;
        for (File f : files) {
            try {
                env.getPropertySources().addFirst(
                    new ResourcePropertySource(new FileSystemResource(f)));
            } catch (IOException e) {
                throw new GaswException("Failed to load configuration file " + f.getName(), e);
            }
        }
    }
}