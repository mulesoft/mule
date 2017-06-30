/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded;

import static java.lang.Thread.currentThread;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.maven.client.api.model.RemoteRepository.newRemoteRepositoryBuilder;
import static org.mule.maven.client.test.MavenTestHelper.createDefaultCommunityMavenConfigurationBuilder;
import static org.mule.maven.client.test.MavenTestHelper.createDefaultEnterpriseMavenConfigurationBuilder;
import static org.mule.maven.client.test.MavenTestHelper.getLocalRepositoryFolder;
import static org.mule.runtime.module.embedded.api.EmbeddedContainer.builder;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.embedded.api.ContainerConfiguration;
import org.mule.runtime.module.embedded.api.EmbeddedContainer;
import org.mule.runtime.module.embedded.internal.classloading.FilteringClassLoader;
import org.mule.runtime.module.embedded.internal.classloading.JdkOnlyClassLoaderFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.junit.rules.TemporaryFolder;

/**
 * Helper class for running embedded tests.
 * 
 * @since 4.0
 */
public class EmbeddedTestHelper {

  private final TemporaryFolder temporaryFolder;
  private final boolean enterprise;
  private File containerFolder;
  private File localRepositoryFolder;
  private EmbeddedContainer container;

  public EmbeddedTestHelper(boolean enterprise) {
    try {
      temporaryFolder = new TemporaryFolder();
      temporaryFolder.create();
      this.localRepositoryFolder = temporaryFolder.newFolder();
      this.enterprise = enterprise;
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
        recreateContainerFolder();
        MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
            enterprise ? createDefaultEnterpriseMavenConfigurationBuilder() : createDefaultCommunityMavenConfigurationBuilder();
        embeddedContainerBuilder = builder()
            .withMuleVersion(System.getProperty("mule.version"))
            .withContainerConfiguration(ContainerConfiguration.builder().withContainerFolder(containerFolder).build())
            .withMavenConfiguration(mavenConfigurationBuilder.withLocalMavenRepositoryLocation(localRepositoryFolder)
                .withRemoteRepository(newRemoteRepositoryBuilder().withId("local.repo")
                    .withUrl(getLocalRepositoryFolder().toURI().toURL())
                    .build())
                .build());
        embeddedContainerConfigurer.accept(embeddedContainerBuilder);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      container = null;
      try {
        container = embeddedContainerBuilder.build();
        container.start();
        test.run();
      } finally {
        if (container != null)
          try {
            container.stop();
            deleteDirectory(containerFolder);
          } catch (Throwable containerStopException) {
            // Never mind
          }
      }
    });
  }

  public File getContainerFolder() {
    return containerFolder;
  }

  public void dispose() {
    temporaryFolder.delete();
  }

  public File getPackagedApplication(File applicationFolder) throws Exception {
    File compressedFile = temporaryFolder.newFile(applicationFolder.getName());
    ZipFile zipFile = new ZipFile(compressedFile.getAbsolutePath() + ".jar");
    File[] files = applicationFolder.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        zipFile.addFolder(file, new ZipParameters());
      } else {
        zipFile.addFile(file, new ZipParameters());
      }
    }
    return zipFile.getFile();
  }

  public File getFolderForApplication(String applicationFolderName) {
    return toFile(getClass().getClassLoader().getResource(applicationFolderName));
  }

  public EmbeddedContainer getContainer() {
    return container;
  }

}
