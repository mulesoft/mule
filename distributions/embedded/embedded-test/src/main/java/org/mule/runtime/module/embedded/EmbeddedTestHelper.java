/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded;

import static java.lang.Thread.currentThread;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.maven.client.api.model.RemoteRepository.newRemoteRepositoryBuilder;
import static org.mule.runtime.module.embedded.api.EmbeddedContainer.builder;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.module.embedded.api.ContainerConfiguration;
import org.mule.runtime.module.embedded.api.EmbeddedContainer;
import org.mule.runtime.module.embedded.internal.classloading.FilteringClassLoader;
import org.mule.runtime.module.embedded.internal.classloading.JdkOnlyClassLoaderFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

/**
 * Helper class for running embedded tests.
 * 
 * @since 4.0
 */
public class EmbeddedTestHelper {

  private static final Logger LOGGER = getLogger(EmbeddedTestHelper.class);
  private File localRepositoryFolder;
  private final TemporaryFolder temporaryFolder;
  private File containerFolder;
  private EmbeddedContainer container;

  public EmbeddedTestHelper() {
    try {
      temporaryFolder = new TemporaryFolder();
      temporaryFolder.create();
      this.containerFolder = temporaryFolder.newFolder();
      this.localRepositoryFolder = temporaryFolder.newFolder();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void recreateContainerFolder() {
    try {
      this.containerFolder = temporaryFolder.newFolder();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the proper context for creating an embedded container
   * 
   * @param runnable a task that will create a {@link org.mule.runtime.module.embedded.api.EmbeddedContainer} for testing
   *        purposes.
   */
  public void test(Runnable runnable) {
    ClassLoader contextClassLoader = currentThread().getContextClassLoader();
    try {
      // Sets a classloader with the JDK only to ensure that dependencies are read form the embedded container classloader
      FilteringClassLoader jdkOnlyClassLoader = JdkOnlyClassLoaderFactory.create();
      currentThread().setContextClassLoader(jdkOnlyClassLoader);

      runnable.run();
    } finally {
      currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  /**
   * Preconfigures the {@link EmbeddedContainer} with default settings
   * 
   * @param embeddedContainerConfigurer function to add configuration to the embedded container
   * @param test function that run the tests
   */
  public void testWithDefaultSettings(Consumer<EmbeddedContainer.EmbeddedContainerBuilder> embeddedContainerConfigurer,
                                      Runnable test) {
    test(() -> {
      EmbeddedContainer.EmbeddedContainerBuilder embeddedContainerBuilder;
      try {
        LOGGER.info("Using folder as local repository: " + localRepositoryFolder.getAbsolutePath());

        embeddedContainerBuilder = builder()
            .withMuleVersion(System.getProperty("mule.version"))
            .withContainerConfiguration(ContainerConfiguration.builder().withContainerFolder(containerFolder).build())
            .withMavenConfiguration(newMavenConfigurationBuilder()
                .withLocalMavenRepositoryLocation(localRepositoryFolder)
                .withRemoteRepository(newRemoteRepositoryBuilder().withId("mulesoft-public")
                    .withUrl(new URL("https://repository.mulesoft.org/nexus/content/repositories/public"))
                    .build())
                .build());
        embeddedContainerConfigurer.accept(embeddedContainerBuilder);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }

      container = null;
      try {
        container = embeddedContainerBuilder.build();
        container.start();
        test.run();
        container.stop();
      } catch (Throwable e) {
        if (container != null)
          try {
            container.stop();
          } catch (Throwable containerStopException) {
            // Never mind
          }
        throw e;
      }
    });
  }

  public File getContainerFolder() {
    return containerFolder;
  }

  public File getLocalRepositoryFolder() {
    return localRepositoryFolder;
  }

  public void dispose() {
    temporaryFolder.delete();
  }

  public File getPackagedApplication(File applicationFolder) throws Exception {
    File compressedFile = temporaryFolder.newFile(applicationFolder.getName());
    ZipFile zipFile = new ZipFile(compressedFile.getAbsolutePath() + ".jar");
    zipFile.addFolder(applicationFolder, new ZipParameters());
    return zipFile.getFile();
  }

  public File getFolderForApplication(String applicationFolderName) {
    return toFile(getClass().getClassLoader().getResource(applicationFolderName));
  }

  public EmbeddedContainer getContainer() {
    return container;
  }
}
