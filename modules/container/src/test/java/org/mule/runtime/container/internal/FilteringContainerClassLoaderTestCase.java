/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.classloader.ExportedService;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoaderTestCase;
import org.mule.tck.classlaoder.TestClassLoader;
import org.mule.tck.util.EnumerationMatcher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;

public class FilteringContainerClassLoaderTestCase extends FilteringArtifactClassLoaderTestCase {

  @Override
  protected FilteringArtifactClassLoader doCreateClassLoader(List<ExportedService> exportedServices) {
    return new FilteringContainerClassLoader(artifactClassLoader, filter, exportedServices);
  }

  public FilteringContainerClassLoaderTestCase(boolean verboseClassloadingLog) {
    super(verboseClassloadingLog);
  }

  @Test
  @Override
  public void loadsExportedResource() throws ClassNotFoundException, MalformedURLException {
    TestClassLoader classLoader = new TestClassLoader(null);
    URL expectedResource = new URL("file:///app.txt");
    classLoader.addResource(FilteringArtifactClassLoaderTestCase.RESOURCE_NAME, expectedResource);

    when(filter.exportsResource(FilteringArtifactClassLoaderTestCase.RESOURCE_NAME)).thenReturn(true);
    when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    URL resource = filteringArtifactClassLoader.getResource(FilteringArtifactClassLoaderTestCase.RESOURCE_NAME);
    assertThat(resource, equalTo(expectedResource));
  }

  @Test
  @Override
  public void getsExportedResources() throws Exception {
    TestClassLoader classLoader = new TestClassLoader(null);
    URL resource = new URL("file:/app.txt");
    classLoader.addResource(FilteringArtifactClassLoaderTestCase.RESOURCE_NAME, resource);

    when(filter.exportsResource(FilteringArtifactClassLoaderTestCase.RESOURCE_NAME)).thenReturn(true);
    when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

    filteringArtifactClassLoader = doCreateClassLoader(emptyList());

    Enumeration<URL> resources = filteringArtifactClassLoader.getResources(FilteringArtifactClassLoaderTestCase.RESOURCE_NAME);
    assertThat(resources, EnumerationMatcher.equalTo(Collections.singletonList(resource)));
  }
}
