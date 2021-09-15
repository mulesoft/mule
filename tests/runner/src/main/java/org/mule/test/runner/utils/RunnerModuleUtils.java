/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.PropertiesUtils.discoverProperties;

import org.mule.runtime.internal.classloader.CompositeClassLoader;
import org.mule.test.runner.api.DependencyResolver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Utility class for runner.
 */
public final class RunnerModuleUtils {

  public static final String EXCLUDED_PROPERTIES_FILE = "excluded.properties";

  public static final String EXCLUDED_ARTIFACTS = "excluded.artifacts";
  public static final String EXTRA_BOOT_PACKAGES = "extraBoot.packages";
  public static final String JAR_EXTENSION = "jar";

  // TODO: MULE-19762 remove once forward compatibility is finished
  private static String DEFAULT_TEST_SDK_API_VERSION_PROPERTY = SYSTEM_PROPERTY_PREFIX + "testSdkApiVersion";
  private static final String SDK_API_GROUP_ID = "org.mule.sdk";
  private static final String SDK_API_ARTIFACT_ID = "mule-sdk-api";
  private static final String DEFAULT_SDK_API_VERSION = getDefaultSdkApiVersionForTest();
  private static final Artifact DEFAULT_SDK_API_ARTIFACT = new DefaultArtifact(SDK_API_GROUP_ID,
                                                                               SDK_API_ARTIFACT_ID,
                                                                               JAR_EXTENSION,
                                                                               DEFAULT_SDK_API_VERSION);


  private RunnerModuleUtils() {}

  /**
   * Loads the {@link RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} resources files, merges the entries so only one
   * {@link Properties} is returned with all values.
   *
   * @return a {@link Properties} loaded with the content of the file.
   * @throws IOException           if the properties couldn't load the file.
   * @throws IllegalStateException if the file couldn't be found.
   */
  public static Properties getExcludedProperties() throws IllegalStateException, IOException {
    Properties excludedProperties = new Properties();
    discoverProperties(EXCLUDED_PROPERTIES_FILE).stream()
        .forEach(properties -> properties.forEach((k, v) -> excludedProperties.merge(k, v, (v1, v2) -> v1 + "," + v2)));
    return excludedProperties;
  }

  /**
   * @return an {@link Artifact} pointing to the default mule-sdk-api.
   */
  public static Artifact getDefaultSdkApiArtifact() {
    return DEFAULT_SDK_API_ARTIFACT;
  }

  /**
   * Tests the {@code extensionClassLoader} for the presence of the {@code mule-sdk-api} classpath and forces it to load it if
   * missing
   *
   * @param extensionClassLoader the extension's classlaoder
   * @param dependencyResolver   a {@link DependencyResolver}
   * @param repositories         the repositories for fetching the mule-sdk-api if missing in the classloader
   * @since 4.5.0
   */
  // TODO: MULE-19762 remove once forward compatibility is finished
  public static ClassLoader assureSdkApiInClassLoader(ClassLoader extensionClassLoader,
                                                      DependencyResolver dependencyResolver,
                                                      List<RemoteRepository> repositories) {
    try {
      Class.forName("org.mule.sdk.api.runtime.parameter.ParameterResolver", true, extensionClassLoader);
      return extensionClassLoader;
    } catch (ClassNotFoundException cnf) {
      try {
        Artifact sdkApiArtifact = dependencyResolver
            .resolveArtifact(getDefaultSdkApiArtifact(), repositories)
            .getArtifact();
        List<File> dependencies = new LinkedList<>();
        dependencies.add(sdkApiArtifact.getFile());
        dependencies.addAll(dependencyResolver.getDirectDependencies(sdkApiArtifact).stream()
            .flatMap(dep -> {
              try {
                return dependencyResolver
                    .resolveDependencies(dep, Collections.emptyList(), Collections.emptyList(), (n, p) -> true, repositories)
                    .stream();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
            .collect(Collectors.toList()));

        URL[] urls = dependencies.stream().map(f -> {
          try {
            return f.getAbsoluteFile().toURL();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }).toArray(URL[]::new);

        extensionClassLoader = CompositeClassLoader.from(extensionClassLoader, new URLClassLoader(urls, extensionClassLoader));
        Class.forName("org.mule.sdk.api.runtime.parameter.ParameterResolver", true, extensionClassLoader);

        return extensionClassLoader;
      } catch (Exception e) {
        throw new RuntimeException("Could not assure sdk-api in extension classloader", e);
      }
    }
  }

  /**
   * @return resolves the default version of {@code mule-sdk-api} to add into the container classpath
   * @sine 4.5.0
   */
  // TODO: MULE-19762 remove once forward compatibility is finished
  private static String getDefaultSdkApiVersionForTest() {
    return "1.0.0-SNAPSHOT";
    // return getProperty(DEFAULT_TEST_SDK_API_VERSION_PROPERTY, "0.4.0");
  }

}
