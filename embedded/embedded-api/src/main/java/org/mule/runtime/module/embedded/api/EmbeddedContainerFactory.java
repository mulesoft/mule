/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.api;

import static org.mule.runtime.module.embedded.internal.MavenUtils.loadUrls;
import static org.mule.runtime.module.embedded.internal.Serializer.serialize;
import org.mule.runtime.module.embedded.internal.MavenContainerClassLoaderFactory;
import org.mule.runtime.module.embedded.internal.Repository;
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

public interface EmbeddedContainerFactory {

  static EmbeddedContainer create(String muleVersion, URL containerBaseFolder, ArtifactInfo application) {
    try {
      Repository repository = new Repository();

      JdkOnlyClassLoader jdkOnlyClassLoader = new JdkOnlyClassLoader();

      MavenContainerClassLoaderFactory classLoaderFactory = new MavenContainerClassLoaderFactory(repository);
      ClassLoader containerModulesClassLoader = classLoaderFactory.create(muleVersion, jdkOnlyClassLoader);

      List<URL> services = classLoaderFactory.getServices(muleVersion);
      ContainerInfo containerInfo = new ContainerInfo(muleVersion, containerBaseFolder, services);

      ClassLoader embeddedControllerBootstrapClassLoader =
          createEmbeddedImplClassLoader(containerModulesClassLoader, repository, muleVersion);

      try {
        Class<?> controllerClass =
            embeddedControllerBootstrapClassLoader.loadClass("org.mule.runtime.module.embedded.impl.EmbeddedController");

        Constructor<?> constructor = controllerClass.getConstructor(byte[].class, byte[].class);
        ByteArrayOutputStream containerOutputStream = new ByteArrayOutputStream(512);
        serialize(containerInfo, containerOutputStream);

        ByteArrayOutputStream appOutputStream = new ByteArrayOutputStream(512);
        serialize(application, appOutputStream);
        Object o = constructor.newInstance(containerOutputStream.toByteArray(), appOutputStream.toByteArray());

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

  static ClassLoader createEmbeddedImplClassLoader(ClassLoader parentClassLoader, Repository repository, String muleVersion)
      throws ArtifactResolutionException, MalformedURLException {
    ArtifactRequest embeddedImplArtifactRequest =
        new ArtifactRequest().setArtifact(new DefaultArtifact("org.mule", "mule-module-embedded-impl", "jar", muleVersion));
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
