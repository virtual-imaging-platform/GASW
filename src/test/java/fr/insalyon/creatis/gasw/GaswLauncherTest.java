package fr.insalyon.creatis.gasw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GaswLauncherTest {

    @Test
    @DisplayName("does nothing when ~/.gasw does not exist")
    void missingConfigDir_doesNothing(@TempDir Path tempHome) throws Exception {
        System.setProperty("user.home", tempHome.toString());
        ConfigurableEnvironment env = mock(ConfigurableEnvironment.class);
        invokeLoadExternalConfig(env);
        verifyNoInteractions(env);
    }

    @Test
    @DisplayName("registers every .properties file in ~/.gasw ahead of the defaults, ignores everything else")
    void loadsOnlyPropertiesFiles_withHighestPrecedence(@TempDir Path tempHome) throws Exception {
        System.setProperty("user.home", tempHome.toString());
        Path gaswDir = Files.createDirectory(tempHome.resolve(".gasw"));
        Files.writeString(gaswDir.resolve("override.properties"), "gasw.vo.name=biomed");
        Files.writeString(gaswDir.resolve("readme.txt"), "not a properties file");

        MutablePropertySources sources = new MutablePropertySources();
        ConfigurableEnvironment env = mock(ConfigurableEnvironment.class);
        when(env.getPropertySources()).thenReturn(sources);
        invokeLoadExternalConfig(env);

        PropertySource<?> ps = sources.stream()
                .filter(p -> p.getName().contains("override.properties"))
                .findFirst()
                .orElseThrow();

        assertEquals("biomed", ps.getProperty("gasw.vo.name"));
        assertEquals(1, sources.stream().count());
    }

    private void invokeLoadExternalConfig(ConfigurableEnvironment env) throws Exception {
        Method method = GaswLauncher.class.getDeclaredMethod("loadExternalConfig", ConfigurableEnvironment.class);
        method.setAccessible(true);
        method.invoke(null, env);
    }
}
