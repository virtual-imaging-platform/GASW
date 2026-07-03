package fr.insalyon.creatis.gasw;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * We need to limit the log size to avoid them to make a full on disk when there is an infinite loop with an error
 * (this happened)
 * At first, I wanted to configure logback to keep only the beginning of the logs and stop once a limit is attained.
 * This does not seem to be possible (easily at least)
 * So I did a classic config, with a simple rolling (only 1 rolled file) and a size limit.
 * So logback will a log file workflow.out (and workflow.err), and move it to workflow-1.out (and workflow-1.err) once
 * it gets bigger than the limit (deleting the previous workflow-1.out/err if they existed).
 *
 * We test that here by :
 * - using a logback-test.xml config file only used in test
 * - ensure that this behaves as expected with a limit of 1KB
 * - then we ensure that the logback.xml config file used in production is almost the same as logback-test.xml except
 * the small stuff necessary for the tests (size of 1KB instead of 100MB, and check every millisecond)
 *
 * Also, to test a clean context, we delete the logs files before the test.
 * We also have to reload logback after that because logback won't recreate the manually deleted log files.
 *
 */
public class GaswLoggerTest {

    @Test
    public void verifyProdConfigIsTheSameAsTheTestOne() throws IOException, URISyntaxException {
        // compare logback-test.xml (used in the next test) and logback.xml (used in prod)
        // only small test stuff must change
        URL prodConfigURL = getClass().getResource("/logback.xml");
        URL testConfigURL = getClass().getResource("/logback-test.xml");
        List<String> prodConfig = Files.readAllLines(Paths.get(prodConfigURL.toURI()));
        List<String> testConfig = Files.readAllLines(Paths.get(testConfigURL.toURI()));

        // in test : remove the checkIncrement lines, flagged with a suffix
        testConfig.removeIf(line -> line.trim().endsWith("<!-- only for test -->"));

        // also change and assert the max size is the expected one
        // assert the size is only configured twice (stdout and stderr)
        Assertions.assertEquals(2, testConfig.stream().filter(line -> line.trim().startsWith("<maxFileSize>")).count());
        testConfig = testConfig.stream().map(line -> {
            if (!line.trim().startsWith("<maxFileSize>")) {
                return line;
            }
            return line.replace("1KB", "100MB");
        }).collect(Collectors.toList());

        Assertions.assertIterableEquals(testConfig, prodConfig);
    }

    @Test
    public void testLogFileSizeLimit() throws IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(Gasw.class);
        // 100 log lines should take around 10KB
        for (int i = 0; i < 100; i++) {
            // wait a little because logback has a timeout and do not verify the size if the logs are too close
            // in logback-test.xml, logcback si configured to test every millisecond
            Thread.sleep(2);
            logger.error(i + " / This test line should take around 100B");
        }
        // verify
        assertLogFiles();
    }

    @BeforeEach
    @AfterEach
    public void cleanLogFiles() throws IOException, JoranException {
        try (Stream<Path> stream = Files.list(Paths.get(""))) {
            stream
                    .filter(file -> !Files.isDirectory(file))
                    .filter(file -> file.getFileName().toString().startsWith("workflow"))
                    .forEach(f -> {
                        System.out.println("deleting : " + f.getFileName());
                        f.toFile().delete();
                    });
        }
        /*
        If we do not reload, as we delete the log file after logback has started (in other tests),
        logback will not re-create them.
         */
        System.out.println("reloading logback");
        reloadLogback();
    }

    public void reloadLogback() throws JoranException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        ContextInitializer initializer = new ContextInitializer(context);
        initializer.autoConfig();
    }

    public void assertLogFiles() throws IOException {
        List<Path> logFiles;
        try (Stream<Path> stream = Files.list(Paths.get(""))) {
            logFiles = stream
                    .filter(file -> !Files.isDirectory(file))
                    .filter(file -> file.getFileName().toString().startsWith("workflow"))
                    .collect(Collectors.toList());

        }
        // we should get only 1 log file and 1 roll-over for .out and .err
        List<String> authorizedNames = Arrays.asList("workflow.out", "workflow-1.out", "workflow.err", "workflow-1.err");
        for (Path logFile : logFiles) {
            Assertions.assertTrue(
                    authorizedNames.contains(logFile.getFileName().toString()),
                    "a log file is not workflow.out or workflow.err : " + logFile);
        }
        // rollover at 1KB. it must be 1 line bigger than 1KB, so not bigger than 1300 bytes with a margin
        for (Path logFile : logFiles) {
            Assertions.assertTrue(
                    Files.size(logFile) < 1300,
                    "a log file is too much bigger than 1KB : " + logFile
                            + " / size " + Files.size(logFile));
        }
        // verify logs are done
        for (String logFileName : authorizedNames) {
            Path logFile = Paths.get(logFileName);
            Assertions.assertTrue(Files.exists(logFile), "Missing log file " + logFile);

            // there could be only 1 line if rolling has just happened
            List<String> logLines = Files.readAllLines(logFile);
            Assertions.assertTrue(logLines.size() > 0, "a log file is too small : " + logFileName);
            Assertions.assertTrue(
                    logLines.get(0).contains("This test line should take around 100B"),
                    "a log file does not contain the test log " + logFileName);
        }
    }
}

