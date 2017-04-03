/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.module.embedded.internal.MavenUtils.loadUrls;
import static org.mule.runtime.module.embedded.internal.Serializer.serialize;
import org.mule.runtime.module.embedded.api.ApplicationConfiguration;
import org.mule.runtime.module.embedded.api.ContainerInfo;
import org.mule.runtime.module.embedded.api.EmbeddedContainer;
import org.mule.runtime.module.embedded.internal.classloading.JdkOnlyClassLoader;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class DefaultEmbeddedContainerBuilder implements EmbeddedContainer.EmbeddedContainerBuilder {

  private String muleVersion;
  private URL containerBaseFolder;
  private ApplicationConfiguration applicationConfigruation;
  private String log4jConfigurationFile;

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
  public EmbeddedContainer build() {
    checkState(muleVersion != null, "muleVersion cannot be null");
    checkState(containerBaseFolder != null, "containerBaseFolder cannot be null");
    checkState(applicationConfigruation != null, "application cannot be null");
    try {
      Repository repository = new Repository();

      JdkOnlyClassLoader jdkOnlyClassLoader = new JdkOnlyClassLoader();

      MavenContainerClassLoaderFactory classLoaderFactory = new MavenContainerClassLoaderFactory(repository);
      ClassLoader containerModulesClassLoader = classLoaderFactory.create(muleVersion, jdkOnlyClassLoader);

      List<URL> services = classLoaderFactory.getServices(muleVersion);
      ContainerInfo containerInfo = new ContainerInfo(muleVersion, containerBaseFolder, services);
      ofNullable(log4jConfigurationFile).ifPresent(containerInfo::setLog4jConfigurationFile);

      ClassLoader embeddedControllerBootstrapClassLoader =
          createEmbeddedImplClassLoader(containerModulesClassLoader, repository, muleVersion);

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

  private static ClassLoader createEmbeddedImplClassLoader(ClassLoader parentClassLoader, Repository repository,
                                                           String muleVersion)
      throws ArtifactResolutionException, MalformedURLException {
    ArtifactRequest embeddedImplArtifactRequest =
        new ArtifactRequest()
            .setArtifact(new DefaultArtifact("org.mule.runtime", "mule-module-embedded-impl", "jar", muleVersion));
    ArtifactResult artifactResult = repository.getSystem().resolveArtifact(repository.getSession(), embeddedImplArtifactRequest);

    PreorderNodeListGenerator preorderNodeListGenerator =
        repository.assemblyDependenciesForArtifact(artifactResult.getArtifact(), artifact -> true);
    List<URL> embeddedUrls = loadUrls(preorderNodeListGenerator);
    embeddedUrls.addAll(embeddedUrls);
    embeddedUrls.add(artifactResult.getArtifact().getFile().toURI().toURL());

    URLClassLoader urlClassLoader = new URLClassLoader(embeddedUrls.toArray(new URL[0]), parentClassLoader);
    return urlClassLoader;
  }

}
