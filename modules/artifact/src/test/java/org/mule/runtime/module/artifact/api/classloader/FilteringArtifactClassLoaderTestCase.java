/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.tck.util.EnumerationMatcher.equalTo;
import org.mule.runtime.core.internal.util.EnumerationAdapter;
import org.mule.runtime.module.artifact.api.classloader.exception.NotExportedClassException;
import org.mule.tck.classlaoder.TestClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FilteringArtifactClassLoaderTestCase extends AbstractMuleTestCase {

  private static final String CLASS_NAME = "java.lang.Object";
  public static final String RESOURCE_NAME = "dummy.txt";
  private static final String SERVICE_INTERFACE_NAME = "org.foo.Service";
  private static final String SERVICE_RESOURCE_NAME = "META-INF/services/" + SERVICE_INTERFACE_NAME;


  @Rule
  public ExpectedException expected = ExpectedException.none();

  public boolean verboseClassloadingLog;
  @Rule
  public SystemProperty verboseClassloading;

  protected FilteringArtifactClassLoader filteringArtifactClassLoader;
  protected final ClassLoaderFilter filter = mock(ClassLoaderFilter.class);
  protected final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);

  @Parameters(name = "verbose: {0}")
  public static Collection<Object[]> params() {
    return asList(new Object[][] {{true}, {false}});
  }

  public FilteringArtifactClassLoaderTestCase(boolean verboseClassloadingLog) {
    this.verboseClassloadingLog = verboseClassloadingLog;
    verboseClassloading = new SystemProperty(MULE_LOG_VERBOSE_CLASSLOADING, Boolean.toString(verboseClassloadingLog));
  }

  @Before
  public void before() {
    when(artifactClassLoader.getArtifactId()).thenReturn("mockArtifact");
  }

  @Test
  public void throwClassNotFoundErrorWhenClassIsNotExported() throws ClassNotFoundException {
    expected.expect(NotExportedClassException.class);
    if (verboseClassloadingLog) {
      expected.expectMessage(is("Class '" + CLASS_NAME + "' not found in classloader for artifact 'mockArtifact'."
          + lineSeparator() + filter.toString()));
    } else {
      expected.expectMessage(is("Class '" + CLASS_NAME + "' not found in classloader for artifact 'mockArtifact'."));
    }

    when(filter.exportsClass(CLASS_NAME)).thenReturn(false);
    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    filteringArtifactClassLoader.loadClass(CLASS_NAME);
  }

  protected FilteringArtifactClassLoader doCreateClassLoader(List<ExportedService> exportedServices) {
    return new FilteringArtifactClassLoader(artifactClassLoader, filter, exportedServices);
  }

  @Test
  public void loadsExportedClass() throws ClassNotFoundException {
    TestClassLoader classLoader = new TestClassLoader(null);
    Class expectedClass = this.getClass();
    classLoader.addClass(CLASS_NAME, expectedClass);

    when(filter.exportsClass(CLASS_NAME)).thenReturn(true);
    when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

    filteringArtifactClassLoader = doCreateClassLoader(emptyList());
    Class<?> aClass = filteringArtifactClassLoader.loadClass(CLASS_NAME);
    assertThat(aClass, equalTo(expectedClass));
  }

  @Test
  public void filtersResourceWhenNotExported() throws ClassNotFoundException {
    when(filter.exportsClass(RESOURCE_NAME)).thenReturn(false);
    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    URL resource = filteringArtifactClassLoader.getResource(RESOURCE_NAME);
    assertThat(resource, CoreMatchers.equalTo(null));
  }

  @Test
  public void loadsExportedResource() throws ClassNotFoundException, IOException {
    URL expectedResource = new URL("file:///app.txt");

    when(filter.exportsResource(RESOURCE_NAME)).thenReturn(true);
    when(artifactClassLoader.findResource(RESOURCE_NAME)).thenReturn(expectedResource);

    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    URL resource = filteringArtifactClassLoader.getResource(RESOURCE_NAME);
    assertThat(resource, equalTo(expectedResource));
  }

  @Test
  public void loadsExportedService() throws ClassNotFoundException, IOException {
    URL expectedResource = new URL("file:///app.txt");

    filteringArtifactClassLoader =
        doCreateClassLoader(singletonList(new ExportedService(SERVICE_INTERFACE_NAME, expectedResource)));

    URL resource = filteringArtifactClassLoader.getResource(SERVICE_RESOURCE_NAME);

    assertThat(resource, equalTo(expectedResource));
    verify(filter, never()).exportsResource(SERVICE_RESOURCE_NAME);
    verify(artifactClassLoader, never()).findResource(SERVICE_RESOURCE_NAME);
  }

  @Test
  public void filtersResources() throws Exception {
    TestClassLoader classLoader = new TestClassLoader(null);
    URL blockedResource = new URL("file:///app.txt");
    classLoader.addResource(RESOURCE_NAME, blockedResource);

    when(filter.exportsResource(RESOURCE_NAME)).thenReturn(false);
    when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    Enumeration<URL> resources = filteringArtifactClassLoader.getResources(RESOURCE_NAME);
    assertThat(resources, equalTo(Collections.EMPTY_LIST));
  }

  @Test
  public void getsExportedResources() throws Exception {
    URL resource = new URL("file:/app.txt");

    when(filter.exportsResource(RESOURCE_NAME)).thenReturn(true);
    when(artifactClassLoader.findResources(RESOURCE_NAME)).thenReturn(new EnumerationAdapter<>(Collections.singleton(resource)));

    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    Enumeration<URL> resources = filteringArtifactClassLoader.getResources(RESOURCE_NAME);
    assertThat(resources, equalTo(Collections.singletonList(resource)));
  }

  @Test
  public void loadsExportedServices() throws ClassNotFoundException, IOException {
    URL expectedResource = new URL("file:///app.txt");

    filteringArtifactClassLoader =
        doCreateClassLoader(singletonList(new ExportedService(SERVICE_INTERFACE_NAME, expectedResource)));

    URL resource = filteringArtifactClassLoader.getResource(SERVICE_RESOURCE_NAME);

    Enumeration<URL> resources = filteringArtifactClassLoader.getResources(SERVICE_RESOURCE_NAME);
    assertThat(resources, equalTo(Collections.singletonList(resource)));

    verify(filter, never()).exportsResource(SERVICE_RESOURCE_NAME);
    verify(artifactClassLoader, never()).findResources(SERVICE_RESOURCE_NAME);
  }

  @Test
  public void returnsCorrectClassLoader() throws Exception {
    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    final ClassLoader classLoader = filteringArtifactClassLoader.getClassLoader();

    assertThat(classLoader, is(filteringArtifactClassLoader));
  }

  @Test
  public void doesNotDisposesFilteredClassLoader() throws Exception {
    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    filteringArtifactClassLoader.dispose();

    verify(artifactClassLoader, never()).dispose();
  }
}
