/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static com.google.common.io.Files.createTempDir;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.tck.MuleTestUtils.testWithSystemProperties;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.collection.DependencyCollectionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Ignore("MULE-12547: Flaky")
public class MavenClassLoaderModelLoaderTestCase {

  public static final String MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION = "muleRuntimeConfig.maven.repositoryLocation";
  private MavenClassLoaderModelLoader mavenClassLoaderModelLoader;

  private File artifactFile = getApplicationFolder("apps/single-dependency");

  @Rule
  public SystemProperty repositoryLocation = new SystemProperty(MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION,
                                                                createTempDir().getAbsolutePath());

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() {
    mavenClassLoaderModelLoader = new MavenClassLoaderModelLoader();
  }

  @Test
  public void noMavenConfiguration() throws Exception {
    expectedException.expect(RuntimeException.class);
    expectedException.expectCause(instanceOf(DependencyCollectionException.class));
    mavenClassLoaderModelLoader.load(artifactFile, emptyMap(), APP);
  }

  @Test
  public void changeMavenConfiguration() throws Exception {
    try {
      mavenClassLoaderModelLoader.load(artifactFile, emptyMap(), APP);
      fail();
    } catch (Exception e) {
      // It is should fail
    }

    // Find any System property for muleRuntimeConfig from previous executions...
    final List<String> muleRuntimeConfig =
        System.getProperties().stringPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith("muleRuntimeConfig") && !propertyName.equals(
                                                                                                         MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION))
            .collect(
                     toList());
    Map<String, String> properties = new HashMap<>();
    // It should clear those properties when running the test
    muleRuntimeConfig.forEach(property -> properties.put(property, null));
    properties.put("muleRuntimeConfig.maven.repositories.mavenCentral.url", "https://repo.maven.apache.org/maven2/");

    testWithSystemProperties(properties,
                             () -> {
                               GlobalConfigLoader.reset();
                               assertThat(mavenClassLoaderModelLoader.load(artifactFile, emptyMap(), APP).getDependencies(),
                                          hasItem(hasProperty("descriptor",
                                                              (hasProperty("artifactId", equalTo("commons-collections"))))));
                             });
  }

  private File getApplicationFolder(String appPath) {
    URL noDependenciesFolderUrl = getClass().getClassLoader().getResource(appPath);
    return new File(noDependenciesFolderUrl.getFile());
  }

}
