/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded;

import static com.mashape.unirest.http.Unirest.post;
import static java.lang.String.valueOf;
import static java.nio.file.Files.delete;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.module.embedded.api.ArtifactInfo;
import org.mule.runtime.module.embedded.api.EmbeddedContainer;
import org.mule.runtime.module.embedded.api.EmbeddedContainerFactory;
import org.mule.tck.junit4.rule.FreePortFinder;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EmbeddedContainerFactoryTestCase {

  public static final String LOGGING_FILE = "app.log";
  @Rule
  public TemporaryFolder containerFolder = new TemporaryFolder();

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
    }, false, empty());
  }

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
    }, false, of(getClass().getClassLoader().getResource("log4j2-custom-file.xml").getFile()));
    try {
      File expectedLoggingFile = new File(LOGGING_FILE);
      assertThat(expectedLoggingFile.exists(), is(true));
      assertThat(expectedLoggingFile.length(), greaterThan(0l));
    } finally {
      delete(Paths.get(LOGGING_FILE));
    }
  }

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
    }, true, empty());
  }

  public void doWithinApplication(String applicaitonFolder, Consumer<Integer> portConsumer, boolean enableTestDependencies,
                                  Optional<String> log4JConfigurationFile)
      throws URISyntaxException, IOException {
    Map<String, String> applicationProperties = new HashMap<>();
    Integer httpListenerPort = new FreePortFinder(6000, 9000).find();
    applicationProperties.put("httpPort", valueOf(httpListenerPort));
    ArtifactInfo application =
        new ArtifactInfo(Collections
            .singletonList(getClasspathResourceAsUri(applicaitonFolder + File.separator + "mule-config.xml")), null,
                         getClasspathResourceAsUri(applicaitonFolder + File.separator + "pom.xml").toURL(),
                         getClasspathResourceAsUri(applicaitonFolder + File.separator + "mule-application.json").toURL(),
                         applicationProperties, enableTestDependencies);

    log4JConfigurationFile = log4JConfigurationFile.isPresent() ? log4JConfigurationFile
        : of(getClass().getClassLoader().getResource("log4j2-default.xml").getFile());

    EmbeddedContainer embeddedContainer =
        EmbeddedContainerFactory.create("4.0.0-SNAPSHOT", log4JConfigurationFile, containerFolder.newFolder().toURI().toURL(),
                                        application);

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
}
