/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation;

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.functional.api.classloading.isolation.ArtifactUrlClassification;
import org.mule.functional.api.classloading.isolation.ClassPathClassifierContext;
import org.mule.functional.api.classloading.isolation.DependenciesGraph;
import org.mule.functional.api.classloading.isolation.MavenArtifact;
import org.mule.functional.api.classloading.isolation.MavenMultiModuleArtifactMapping;
import org.mule.functional.api.classloading.isolation.ServiceUrlClassification;
import org.mule.functional.classloading.isolation.classification.DefaultClassPathClassifier;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class DefaultClassPathClassifierTestCase extends AbstractMuleTestCase {

  public static final String ECHO_SERVICE_CLASS = "org.mule.service.EchoService";
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = none();
  private DefaultClassPathClassifier classPathClassifier;
  @Mock
  private ClassPathClassifierContext context;
  @Mock
  private DependenciesGraph dependencyGraph;
  @Mock
  private File rootArtifactClassesFolder;
  @Mock
  private File getRootArtifactTestClassesFolder;
  @Mock
  private MavenMultiModuleArtifactMapping mapping;

  @Before
  public void before() throws IOException {
    classPathClassifier = new DefaultClassPathClassifier();

    when(context.getDependencyGraph()).thenReturn(dependencyGraph);
    when(context.getExtensionBasePackages()).thenReturn(Lists.newArrayList("org.mule.invalid"));
    when(context.getRootArtifactClassesFolder()).thenReturn(rootArtifactClassesFolder);
    when(rootArtifactClassesFolder.exists()).thenReturn(false);

    Properties serviceProperties = new Properties();
    serviceProperties.setProperty(DefaultClassPathClassifier.SERVICE_PROVIDER_CLASS_NAME, ECHO_SERVICE_CLASS);
    File metaInfFolder = new File(folder.getRoot(), "META-INF");
    assertThat(metaInfFolder.mkdir(), is(true));

    serviceProperties
        .store(new FileWriter(new File(metaInfFolder, DefaultClassPathClassifier.SERVICE_PROPERTIES_FILE_NAME)),
               "Service properties for discover process");

    when(context.getClassPathClassLoader()).thenReturn(new URLClassLoader(new URL[] {folder.getRoot().toURI().toURL()}, null));

  }

  /**
   * Validates the logic to discover extensions by scanning locations of service.properties files.
   */
  @Test
  public void testServiceDiscoverProcess() {
    MavenArtifact serviceArtifact = new MavenArtifact.MavenArtifactBuilder().withGroupId("org.mule").withArtifactId("service")
        .withType("jar").withScope("provided").build();

    String artifactPath = folder.getRoot().getParentFile().getParentFile().getAbsolutePath() + separator;
    when(mapping.getArtifactId(artifactPath)).thenReturn(serviceArtifact.getArtifactId());
    when(context.getMavenMultiModuleArtifactMapping()).thenReturn(mapping);
    when(context.getServicesExclusion()).thenReturn(Lists.newArrayList());

    File serviceGroupIdFolder = new File(folder.getRoot(), serviceArtifact.getGroupId());
    assertThat(serviceGroupIdFolder.mkdir(), is(true));
    File serviceArtifactIdFolder = new File(serviceGroupIdFolder, serviceArtifact.getArtifactId());
    assertThat(serviceArtifactIdFolder.mkdir(), is(true));
    when(context.getClassPathURLs()).thenReturn(Lists.newArrayList());

    MavenArtifact rootArtifact = new MavenArtifact.MavenArtifactBuilder().withGroupId("org.mule").withArtifactId("service-test")
        .withType("jar").withScope("compile").build();
    when(context.getDependencyGraph().getRootArtifact()).thenReturn(rootArtifact);
    when(context.getDependencyGraph().getDependencies()).thenReturn(Sets.newHashSet());

    ArtifactUrlClassification classification = classPathClassifier.classify(context);
    assertThat(classification.getServiceUrlClassifications().size(), is(1));
    ServiceUrlClassification serviceUrlClassification = classification.getServiceUrlClassifications().get(0);
    // No need to test this logic as it is already tested in unit tests for dependency resolver code
    assertThat(serviceUrlClassification.getUrls().size(), is(0));
    assertThat(serviceUrlClassification.getName(), equalTo(ECHO_SERVICE_CLASS));

    verify(context, times(8)).getDependencyGraph();
    verify(context).getExtensionBasePackages();
    verify(context, times(2)).getClassPathURLs();
    verify(context, times(2)).getMavenMultiModuleArtifactMapping();
    verify(context).getServicesExclusion();
    verify(context).getClassPathClassLoader();
    verify(context).getRootArtifactClassesFolder();
    verify(dependencyGraph, times(6)).getRootArtifact();
    verify(dependencyGraph, times(3)).getDependencies();
    verify(rootArtifactClassesFolder).exists();
    verify(mapping).getArtifactId(artifactPath);
  }

  /**
   * Validates that a discovered service is not classified due to it is configure to be excluded.
   */
  @Test
  public void testExcludedServiceDiscoverProcess() throws IOException {
    MavenArtifact serviceArtifact = new MavenArtifact.MavenArtifactBuilder().withGroupId("org.mule").withArtifactId("service")
        .withType("jar").withScope("provided").build();

    String artifactPath = folder.getRoot().getParentFile().getParentFile().getAbsolutePath() + separator;
    when(mapping.getArtifactId(artifactPath)).thenReturn(serviceArtifact.getArtifactId());
    when(context.getMavenMultiModuleArtifactMapping()).thenReturn(mapping);
    when(context.getServicesExclusion()).thenReturn(Lists.newArrayList("service"));

    File serviceGroupIdFolder = new File(folder.getRoot(), serviceArtifact.getGroupId());
    assertThat(serviceGroupIdFolder.mkdir(), is(true));
    File serviceArtifactIdFolder = new File(serviceGroupIdFolder, serviceArtifact.getArtifactId());
    assertThat(serviceArtifactIdFolder.mkdir(), is(true));
    when(context.getClassPathURLs()).thenReturn(Lists.newArrayList());

    MavenArtifact rootArtifact = new MavenArtifact.MavenArtifactBuilder().withGroupId("org.mule").withArtifactId("service-test")
        .withType("jar").withScope("compile").build();
    when(context.getDependencyGraph().getRootArtifact()).thenReturn(rootArtifact);

    ArtifactUrlClassification classification = classPathClassifier.classify(context);
    assertThat(classification.getServiceUrlClassifications().size(), is(0));

    verify(context, times(6)).getDependencyGraph();
    verify(context).getExtensionBasePackages();
    verify(context, times(2)).getClassPathURLs();
    verify(context, times(2)).getMavenMultiModuleArtifactMapping();
    verify(context).getServicesExclusion();
    verify(context).getClassPathClassLoader();
    verify(context).getRootArtifactClassesFolder();
    verify(dependencyGraph, times(5)).getRootArtifact();
    verify(dependencyGraph, times(2)).getDependencies();
    verify(rootArtifactClassesFolder).exists();
    verify(mapping).getArtifactId(artifactPath);
  }

  /**
   * Validates that a discovered service cannot be the root artifact
   */
  @Test
  public void testRootArtifactCannotBeDiscoveredAsService() throws IOException {
    MavenArtifact serviceArtifact = new MavenArtifact.MavenArtifactBuilder().withGroupId("org.mule").withArtifactId("service")
        .withType("jar").withScope("provided").build();

    String artifactPath = folder.getRoot().getParentFile().getParentFile().getAbsolutePath() + separator;
    when(mapping.getArtifactId(artifactPath)).thenReturn(serviceArtifact.getArtifactId());
    when(context.getMavenMultiModuleArtifactMapping()).thenReturn(mapping);
    when(context.getServicesExclusion()).thenReturn(Lists.newArrayList());

    File serviceGroupIdFolder = new File(folder.getRoot(), serviceArtifact.getGroupId());
    assertThat(serviceGroupIdFolder.mkdir(), is(true));
    File serviceArtifactIdFolder = new File(serviceGroupIdFolder, serviceArtifact.getArtifactId());
    assertThat(serviceArtifactIdFolder.mkdir(), is(true));
    when(context.getClassPathURLs()).thenReturn(Lists.newArrayList());

    when(context.getDependencyGraph().getRootArtifact()).thenReturn(serviceArtifact);

    expectedException.expect(IllegalArgumentException.class);
    expectedException
        .expectMessage("RootArtifact: '" + serviceArtifact.toString() + "' cannot be a service. It is not supported");
    classPathClassifier.classify(context);
  }
}
