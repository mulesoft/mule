/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded;

import static com.mashape.unirest.http.Unirest.post;
import static java.lang.String.valueOf;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.delete;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.maven.client.api.model.RemoteRepository.newRemoteRepositoryBuilder;
import static org.mule.runtime.api.deployment.management.ComponentInitialStateManager.DISABLE_SCHEDULER_SOURCES_PROPERTY;
import static org.mule.runtime.core.util.UUID.getUUID;
import static org.mule.runtime.module.embedded.api.EmbeddedContainer.builder;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DEPLOYMENT_TYPE;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DeploymentTypeStory.EMBEDDED;
import static org.mule.test.allure.AllureConstants.EmbeddedApiFeature.EMBEDDED_API;
import static org.mule.test.allure.AllureConstants.EmbeddedApiFeature.EmbeddedApiStory.CONFIGURATION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.runtime.module.embedded.api.Application;
import org.mule.runtime.module.embedded.api.ApplicationConfiguration;
import org.mule.runtime.module.embedded.api.DeploymentConfiguration;
import org.mule.runtime.module.embedded.api.EmbeddedContainer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.FreePortFinder;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features({EMBEDDED_API, DEPLOYMENT_TYPE})
@Stories({CONFIGURATION, EMBEDDED})
public class EmbeddedContainerTestCase extends AbstractMuleTestCase {

  private static final String LOGGING_FILE = "app.log";
  private static final Logger LOGGER = getLogger(EmbeddedContainerTestCase.class);

  @ClassRule
  public static TemporaryFolder localRepositoryFolder = new TemporaryFolder();
  @Rule
  public TemporaryFolder containerFolder = new TemporaryFolder();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Description("Embedded runs an application depending on a connector")
  @Test
  public void applicationWithConnector() throws Exception {
    doWithinApplication("http-echo", port -> {
      try {
        String httpBody = "test-message";
        HttpResponse<String> response = post(String.format("http://localhost:%s/", port)).body(httpBody).asString();
        assertThat(response.getBody(), is(httpBody));
      } catch (UnirestException e) {
        throw new RuntimeException(e);
      }
    }, emptyMap());
  }

  @Description("Embedded runs an application using test dependencies")
  @Test
  public void applicationWithTestDependency() throws Exception {
    doWithinApplication("http-test-dependency", port -> {
      try {
        String httpBody = "org.mobicents.xcap.client.impl.XcapClientImpl";
        HttpResponse<String> response = post(String.format("http://localhost:%s/", port)).body(httpBody).asString();
        assertThat(response.getBody(), is(httpBody));
      } catch (UnirestException e) {
        throw new RuntimeException(e);
      }
    }, true, emptyMap(), empty());
  }

  @Description("Embedded runs an application with scheduler not started by using the " + DISABLE_SCHEDULER_SOURCES_PROPERTY
      + " property in the mule-artifact.properties file")
  @Test
  public void applicationWithSchedulersStoppedByDefaultUsingApplicationProperties() throws Exception {
    HashMap<String, String> applicationProperties = new HashMap<>();
    File fileWriteFolder = temporaryFolder.newFolder();
    File fileWriteDestination = new File(fileWriteFolder, getUUID());
    applicationProperties.put("file.path", fileWriteDestination.getAbsolutePath());
    applicationProperties.put(DISABLE_SCHEDULER_SOURCES_PROPERTY, "true");

    // start and stops the application, the scheduler within it should have been run if started
    doWithinApplication("scheduler-stopped", port -> {
      waitForPollToBeExecuted();
    }, applicationProperties);

    assertThat(fileWriteDestination.exists(), is(false));
  }

  @Description("Embedded runs an application with scheduler not started by using the " + DISABLE_SCHEDULER_SOURCES_PROPERTY
      + " property as system property")
  @Test
  public void applicationWithSchedulersStoppedByDefaultUsingSystemProperties() throws Exception {
    HashMap<String, String> applicationProperties = new HashMap<>();
    File fileWriteFolder = temporaryFolder.newFolder();
    File fileWriteDestination = new File(fileWriteFolder, getUUID());
    applicationProperties.put("file.path", fileWriteDestination.getAbsolutePath());

    // start and stops the application, the scheduler within it should have been run if started
    testWithSystemProperty(DISABLE_SCHEDULER_SOURCES_PROPERTY, "true", () -> {
      doWithinApplication("scheduler-stopped", port -> {
        waitForPollToBeExecuted();
      }, applicationProperties);
    });

    assertThat(fileWriteDestination.exists(), is(false));
  }

  @Description("Embedded runs an application using a custom log4j configuration file")
  @Test
  public void applicationWithCustomLogger() throws Exception {
    doWithinApplication("http-echo", port -> {
      try {
        String httpBody = "test-message";
        HttpResponse<String> response = post(String.format("http://localhost:%s/", port)).body(httpBody).asString();
        assertThat(response.getBody(), is(httpBody));
      } catch (UnirestException e) {
        throw new RuntimeException(e);
      }
    }, false, emptyMap(), of(getClass().getClassLoader().getResource("log4j2-custom-file.xml").getFile()));
    try {
      File expectedLoggingFile = new File(LOGGING_FILE);
      assertThat(expectedLoggingFile.exists(), is(true));
      assertThat(expectedLoggingFile.length(), greaterThan(0l));
    } finally {
      delete(Paths.get(LOGGING_FILE));
    }
  }

  private void waitForPollToBeExecuted() {
    try {
      sleep(200);
    } catch (InterruptedException e) {
      // do nothing
    }
  }

  private void doWithinApplication(String applicaitonFolder, Consumer<Integer> portConsumer,
                                   Map<String, String> applicationProperties)
      throws URISyntaxException, IOException {
    doWithinApplication(applicaitonFolder, portConsumer, false, applicationProperties, empty());
  }

  private void doWithinApplication(String applicaitonFolder, Consumer<Integer> portConsumer, boolean enableTestDependencies,
                                   Map<String, String> applicationProperties, Optional<String> log4JConfigurationFileOptional)
      throws URISyntaxException, IOException {
    Map<String, String> customizedApplicationProperties = new HashMap<>(applicationProperties);
    Integer httpListenerPort = new FreePortFinder(6000, 9000).find();
    customizedApplicationProperties.put("httpPort", valueOf(httpListenerPort));
    Application application =
        new Application(
                        singletonList(getClasspathResourceAsUri(applicaitonFolder + File.separator + "mule-config.xml")), null,
                        getClasspathResourceAsUri(applicaitonFolder + File.separator + "pom.xml").toURL(),
                        getClasspathResourceAsUri(applicaitonFolder + File.separator + "mule-application.json").toURL());

    File localRepositoryLocation = localRepositoryFolder.getRoot();
    localRepositoryLocation = MavenClientProvider.discoverProvider(getClass().getClassLoader())
        .getLocalRepositorySuppliers().environmentMavenRepositorySupplier().get();

    LOGGER.info("Using folder as local repository: " + localRepositoryLocation.getAbsolutePath());

    EmbeddedContainer embeddedContainer = builder()
        .withMuleVersion("4.0.0-SNAPSHOT")
        .withContainerBaseFolder(containerFolder.newFolder().toURI().toURL())
        .withMavenConfiguration(newMavenConfigurationBuilder()
            .withLocalMavenRepositoryLocation(localRepositoryLocation)
            .withRemoteRepository(newRemoteRepositoryBuilder().withId("mulesoft-public")
                .withUrl(new URL("https://repository.mulesoft.org/nexus/content/repositories/public"))
                .build())
            .build())
        .withLog4jConfigurationFile(log4JConfigurationFileOptional
            .orElse(getClass().getClassLoader().getResource("log4j2-default.xml").getFile()))
        .withApplicationConfiguration(ApplicationConfiguration.builder()
            .withApplication(application)
            .withDeploymentConfiguration(DeploymentConfiguration.builder()
                .withTestDependenciesEnabled(enableTestDependencies)
                .withArtifactProperties(customizedApplicationProperties)
                .build())
            .build())
        .build();

    embeddedContainer.start();
    try {
      portConsumer.accept(httpListenerPort);
    } finally {
      embeddedContainer.stop();
    }
  }

  private URI getClasspathResourceAsUri(String resource) throws URISyntaxException {
    return getClass().getClassLoader().getResource(resource).toURI();
  }

  @Override
  public int getTestTimeoutSecs() {
    return 20 * super.getTestTimeoutSecs();
  }
}
