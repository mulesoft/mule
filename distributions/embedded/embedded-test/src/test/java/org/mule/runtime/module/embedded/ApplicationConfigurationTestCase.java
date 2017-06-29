/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded;

import static com.mashape.unirest.http.Unirest.post;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.delete;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.deployment.management.ComponentInitialStateManager.DISABLE_SCHEDULER_SOURCES_PROPERTY;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppsFolder;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.module.embedded.api.Product.MULE;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DEPLOYMENT_TYPE;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DeploymentTypeStory.EMBEDDED;
import static org.mule.test.allure.AllureConstants.EmbeddedApiFeature.EMBEDDED_API;
import static org.mule.test.allure.AllureConstants.EmbeddedApiFeature.EmbeddedApiStory.CONFIGURATION;
import org.mule.runtime.module.embedded.api.ArtifactConfiguration;
import org.mule.runtime.module.embedded.api.DeploymentConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.FreePortFinder;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features({EMBEDDED_API, DEPLOYMENT_TYPE})
@Stories({CONFIGURATION, EMBEDDED})
public class ApplicationConfigurationTestCase extends AbstractMuleTestCase {

  private static final String LOGGING_FILE = "app.log";

  private static EmbeddedTestHelper embeddedTestHelper = new EmbeddedTestHelper(false);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @AfterClass
  public static void dispose() {
    embeddedTestHelper.dispose();
  }

  @Description("Embedded runs an application depending on a connector")
  @Test
  public void applicationWithConnector() throws Exception {
    doWithinApplication(embeddedTestHelper.getFolderForApplication("http-echo"), port -> {
      try {
        String httpBody = "test-message";
        HttpResponse<String> response = post(format("http://localhost:%s/", port)).body(httpBody).asString();
        assertThat(response.getBody(), is(httpBody));
      } catch (UnirestException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Description("Embedded runs an application using test dependencies and deploying a jar file")
  @Test
  public void applicationWithTestDependency() throws Exception {
    doWithinApplication(embeddedTestHelper
        .getPackagedApplication(embeddedTestHelper.getFolderForApplication("http-test-dependency")), port -> {
          try {
            String httpBody = "org.mobicents.xcap.client.impl.XcapClientImpl";
            HttpResponse<String> response = post(format("http://localhost:%s/", port)).body(httpBody).asString();
            assertThat(response.getBody(), is(httpBody));
          } catch (UnirestException e) {
            throw new RuntimeException(e);
          }
        }, true, empty());
  }

  @Description("Embedded runs an application with scheduler not started by using the " + DISABLE_SCHEDULER_SOURCES_PROPERTY
      + " property as system property")
  @Test
  public void applicationWithSchedulersStoppedByDefaultUsingSystemProperties() throws Exception {
    File fileWriteFolder = temporaryFolder.newFolder();
    File fileWriteDestination = new File(fileWriteFolder, getUUID());

    // start and stops the application, the scheduler within it should have been run if started
    testWithSystemProperty("file.path", fileWriteDestination.getAbsolutePath(), () -> {
      testWithSystemProperty(DISABLE_SCHEDULER_SOURCES_PROPERTY, "true", () -> {
        doWithinApplication(embeddedTestHelper.getFolderForApplication("scheduler-stopped"), port -> {
          waitForPollToBeExecuted();
        });
      });
    });
    assertThat(fileWriteDestination.exists(), is(false));
  }

  @Description("Embedded runs an application using a custom log4j configuration file")
  @Test
  public void applicationWithCustomLogger() throws Exception {
    doWithinApplication(embeddedTestHelper.getFolderForApplication("http-echo"), port -> {
      try {
        String httpBody = "test-message";
        HttpResponse<String> response = post(format("http://localhost:%s/", port)).body(httpBody).asString();
        assertThat(response.getBody(), is(httpBody));
      } catch (UnirestException e) {
        throw new RuntimeException(e);
      }
    }, false, of(getClass().getClassLoader().getResource("log4j2-custom-file.xml").toURI()));
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

  private void doWithinApplication(File applicationFolder, Consumer<Integer> portConsumer)
      throws Exception {
    doWithinApplication(applicationFolder, portConsumer, false, empty());
  }

  private void doWithinApplication(File applicationFolder, Consumer<Integer> portConsumer, boolean enableTestDependencies,
                                   Optional<URI> log4JConfigurationFileOptional)
      throws Exception {

    Integer httpListenerPort = new FreePortFinder(6000, 9000).find();
    testWithSystemProperty("httpPort", valueOf(httpListenerPort), () -> {
      embeddedTestHelper.recreateContainerFolder();
      embeddedTestHelper.testWithDefaultSettings(embeddedContainerBuilder -> {
        try {
          embeddedContainerBuilder.withLog4jConfigurationFile(log4JConfigurationFileOptional
              .orElse(getClass().getClassLoader().getResource("log4j2-default.xml").toURI()))
              .withProduct(MULE)
              .build();

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }, () -> {
        ArtifactConfiguration applicationConfiguration = ArtifactConfiguration.builder()
            .withArtifactLocation(applicationFolder)
            .withDeploymentConfiguration(DeploymentConfiguration.builder()
                .withTestDependenciesEnabled(enableTestDependencies)
                .build())
            .build();
        embeddedTestHelper.getContainer().getDeploymentService().deployApplication(applicationConfiguration);
        assertThat(new File(getAppsFolder(), applicationFolder.getName().replace(".jar", "")).exists(), is(true));
      });
    });

  }

  @Override
  public int getTestTimeoutSecs() {
    return 20 * super.getTestTimeoutSecs();
  }
}
