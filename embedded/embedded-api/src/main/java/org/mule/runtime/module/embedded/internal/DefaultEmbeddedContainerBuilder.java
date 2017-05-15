/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static org.codehaus.plexus.util.FileUtils.fileWrite;
import static org.codehaus.plexus.util.FileUtils.toFile;
import static org.mule.maven.client.api.model.BundleScope.PROVIDED;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.runtime.module.embedded.internal.Serializer.serialize;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.embedded.api.ApplicationConfiguration;
import org.mule.runtime.module.embedded.api.ContainerInfo;
import org.mule.runtime.module.embedded.api.EmbeddedContainer;
import org.mule.runtime.module.embedded.internal.classloading.JdkOnlyClassLoader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class DefaultEmbeddedContainerBuilder implements EmbeddedContainer.EmbeddedContainerBuilder {

  private String muleVersion;
  private URL containerBaseFolder;
  private ApplicationConfiguration applicationConfigruation;
  private String log4jConfigurationFile;
  private MavenConfiguration mavenConfiguration;

  @Override
  public EmbeddedContainer.EmbeddedContainerBuilder withMuleVersion(String muleVersion) {
    this.muleVersion = muleVersion;
    return this;
  }

  @Override
  public EmbeddedContainer.EmbeddedContainerBuilder withContainerBaseFolder(URL containerBaseFolder) {
    this.containerBaseFolder = containerBaseFolder;
    return this;
  }

  @Override
  public EmbeddedContainer.EmbeddedContainerBuilder withApplicationConfiguration(ApplicationConfiguration applicationConfigruation) {
    this.applicationConfigruation = applicationConfigruation;
    return this;
  }

  @Override
  public EmbeddedContainer.EmbeddedContainerBuilder withLog4jConfigurationFile(String log4JConfigurationFile) {
    this.log4jConfigurationFile = log4JConfigurationFile;
    return this;
  }

  @Override
  public EmbeddedContainer.EmbeddedContainerBuilder withMavenConfiguration(MavenConfiguration mavenConfiguration) {
    this.mavenConfiguration = mavenConfiguration;
    return this;
  }

  @Override
  public EmbeddedContainer build() {
    checkState(muleVersion != null, "muleVersion cannot be null");
    checkState(containerBaseFolder != null, "containerBaseFolder cannot be null");
    checkState(applicationConfigruation != null, "application cannot be null");
    checkState(mavenConfiguration != null, "mavenConfiguration cannot be null");
    try {
      JdkOnlyClassLoader jdkOnlyClassLoader = new JdkOnlyClassLoader();

      if (log4jConfigurationFile != null) {
        configureLogging(jdkOnlyClassLoader);
      }

      MavenClientProvider mavenClientProvider = discoverProvider(getClass().getClassLoader());
      MavenClient mavenClient = mavenClientProvider.createMavenClient(mavenConfiguration);

      MavenContainerClassLoaderFactory classLoaderFactory = new MavenContainerClassLoaderFactory(mavenClient);
      ClassLoader containerModulesClassLoader = classLoaderFactory.create(muleVersion, jdkOnlyClassLoader, containerBaseFolder);

      List<URL> services = classLoaderFactory.getServices(muleVersion);
      ContainerInfo containerInfo = new ContainerInfo(muleVersion, containerBaseFolder, services);

      if (mavenConfiguration != null) {
        persistMavenConfiguration(containerBaseFolder, mavenConfiguration);
      }

      ClassLoader embeddedControllerBootstrapClassLoader =
          createEmbeddedImplClassLoader(containerModulesClassLoader, mavenClient, muleVersion);

      try {
        Class<?> controllerClass =
            embeddedControllerBootstrapClassLoader.loadClass("org.mule.runtime.module.embedded.impl.EmbeddedController");

        Constructor<?> constructor = controllerClass.getConstructor(byte[].class, byte[].class);
        ByteArrayOutputStream containerOutputStream = new ByteArrayOutputStream(512);
        serialize(containerInfo, containerOutputStream);

        ByteArrayOutputStream appConfigOutputStream = new ByteArrayOutputStream(512);
        serialize(applicationConfigruation, appConfigOutputStream);
        Object o = constructor.newInstance(containerOutputStream.toByteArray(), appConfigOutputStream.toByteArray());

        return new EmbeddedContainer() {

          @Override
          public void start() {
            try {
              Method startMethod = o.getClass().getMethod("start");
              startMethod.invoke(o);
            } catch (InvocationTargetException e) {
              Throwable cause = e.getCause();
              if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
              } else {
                throw new IllegalStateException(cause);
              }
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          }

          @Override
          public void stop() {
            try {
              Method stopMethod = o.getClass().getMethod("stop");
              stopMethod.invoke(o);
            } catch (InvocationTargetException e) {
              Throwable cause = e.getCause();
              if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
              } else {
                throw new IllegalStateException(cause);
              }
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          }
        };
      } catch (Exception e) {
        throw new IllegalStateException("Cannot create embedded container", e);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void configureLogging(JdkOnlyClassLoader jdkOnlyClassLoader)
      throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    final Class<?> log4jLogManagerClass = jdkOnlyClassLoader.loadClass("org.apache.logging.log4j.LogManager");
    final Object logContext = log4jLogManagerClass.getMethod("getContext", boolean.class).invoke(null, false);

    final Class<?> log4jLoggerContextClass = jdkOnlyClassLoader.loadClass("org.apache.logging.log4j.core.LoggerContext");
    log4jLoggerContextClass.getMethod("setConfigLocation", URI.class).invoke(logContext,
                                                                             new File(log4jConfigurationFile).toURI());
  }

  private void persistMavenConfiguration(URL containerBaseFolder, MavenConfiguration mavenConfiguration) throws IOException {
    File configurationFolder = new File(toFile(containerBaseFolder), "conf");
    if (!configurationFolder.exists()) {
      if (!configurationFolder.mkdirs()) {
        throw new IllegalArgumentException("Could not create MULE_HOME/conf folder in: " + configurationFolder.getAbsolutePath());
      }
    }

    JsonObject rootObject = new JsonObject();
    JsonObject muleRuntimeConfigObject = new JsonObject();
    rootObject.add("muleRuntimeConfig", muleRuntimeConfigObject);
    JsonObject mavenObject = new JsonObject();
    muleRuntimeConfigObject.add("maven", mavenObject);
    if (!mavenConfiguration.getMavenRemoteRepositories().isEmpty()) {
      JsonObject repositoriesObject = new JsonObject();
      mavenObject.add("repositories", repositoriesObject);
      mavenConfiguration.getMavenRemoteRepositories().forEach(mavenRepo -> {
        JsonObject repoObject = new JsonObject();
        repositoriesObject.add(mavenRepo.getId(), repoObject);
        repoObject.addProperty("url", mavenRepo.getUrl().toString());
        mavenRepo.getAuthentication().ifPresent(authentication -> {
          repoObject.addProperty("username", authentication.getUsername());
          repoObject.addProperty("password", authentication.getPassword());
        });
      });
    }
    mavenObject.addProperty("repositoryLocation", mavenConfiguration.getLocalMavenRepositoryLocation().getAbsolutePath());
    String muleConfigContent = new Gson().toJson(rootObject);
    fileWrite(new File(configurationFolder, "mule-config.json"), muleConfigContent);
  }

  private static ClassLoader createEmbeddedImplClassLoader(ClassLoader parentClassLoader, MavenClient mavenClient,
                                                           String muleVersion)
      throws MalformedURLException {

    BundleDescriptor embeddedControllerImplDescriptor = new BundleDescriptor.Builder().setGroupId("org.mule.runtime")
        .setArtifactId("mule-module-embedded-impl").setVersion(muleVersion).setType("jar").build();

    BundleDependency embeddedBundleImplDependency = mavenClient.resolveBundleDescriptor(embeddedControllerImplDescriptor);

    List<BundleDependency> embeddedImplDependencies =
        mavenClient.resolveBundleDescriptorDependencies(false, embeddedControllerImplDescriptor);

    List<URL> embeddedUrls = embeddedImplDependencies.stream()
        .filter(bundleDependency -> !bundleDependency.getScope().equals(PROVIDED))
        .map(BundleDependency::getBundleUrl)
        .collect(toList());
    embeddedUrls = new ArrayList<>(embeddedUrls);
    embeddedUrls.add(embeddedBundleImplDependency.getBundleUrl());

    URLClassLoader urlClassLoader = new URLClassLoader(embeddedUrls.toArray(new URL[embeddedUrls.size()]), parentClassLoader);
    return urlClassLoader;
  }

}
