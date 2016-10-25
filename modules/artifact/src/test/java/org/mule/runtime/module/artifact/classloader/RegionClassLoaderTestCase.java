/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class RegionClassLoaderTestCase extends AbstractMuleTestCase {

  public static final String PACKAGE_NAME = "java.lang";
  public static final String CLASS_NAME = PACKAGE_NAME + ".Object";
  public static final Class PARENT_LOADED_CLASS = Object.class;
  public static final Class PLUGIN_LOADED_CLASS = String.class;

  public static final String RESOURCE_NAME = "dummy.txt";
  public static final String APP_NAME = "testApp";
  private static final String ARTIFACT_ID = "testAppId";

  public final URL APP_LOADED_RESOURCE;
  public final URL PLUGIN_LOADED_RESOURCE;
  public final URL PARENT_LOADED_RESOURCE;

  private final TestApplicationClassLoader appClassLoader = new TestApplicationClassLoader();
  private final TestArtifactClassLoader pluginClassLoader = new SubTestClassLoader();
  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
  private final ArtifactDescriptor artifactDescriptor;


  public RegionClassLoaderTestCase() throws MalformedURLException {
    PARENT_LOADED_RESOURCE = new URL("file:///parent.txt");
    APP_LOADED_RESOURCE = new URL("file:///app.txt");
    PLUGIN_LOADED_RESOURCE = new URL("file:///plugin.txt");
    artifactDescriptor = new ArtifactDescriptor(APP_NAME);
  }

  @Test(expected = ClassNotFoundException.class)
  public void failsToLoadClassWhenIsNotDefinedInAnyClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.loadClass(CLASS_NAME)).thenThrow(new ClassNotFoundException());

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    List<ArtifactClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

    classLoaders.forEach(classLoader -> regionClassLoader.addClassLoader(classLoader, NULL_CLASSLOADER_FILTER));

    when(lookupPolicy.getLookupStrategy(Object.class.getName())).thenReturn(CHILD_FIRST);
    regionClassLoader.loadClass(CLASS_NAME);
  }

  @Test
  public void loadsParentClassWhenIsNotDefinedInAnyRegionClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.loadClass(CLASS_NAME)).thenReturn(PARENT_LOADED_CLASS);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    List<ArtifactClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

    classLoaders.forEach(classLoader -> regionClassLoader.addClassLoader(classLoader, NULL_CLASSLOADER_FILTER));
    when(lookupPolicy.getLookupStrategy(Object.class.getName())).thenReturn(CHILD_FIRST);
    final Class loadedClass = regionClassLoader.loadClass(CLASS_NAME);
    assertThat(loadedClass, equalTo(PARENT_LOADED_CLASS));
  }

  @Test
  public void loadsClassFromRegionMemberWhenPackageMappingDefined() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.loadClass(CLASS_NAME)).thenReturn(PARENT_LOADED_CLASS);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(singleton(PACKAGE_NAME), emptySet()));
    pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);
    when(lookupPolicy.getLookupStrategy(Object.class.getName())).thenReturn(CHILD_FIRST);
    final Class loadedClass = regionClassLoader.loadClass(CLASS_NAME);
    assertThat(loadedClass, equalTo(PLUGIN_LOADED_CLASS));
  }

  @Test
  public void returnsNullResourceWhenIsNotDefinedInAnyClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(null);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    List<ArtifactClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

    classLoaders.forEach(classLoader -> regionClassLoader.addClassLoader(classLoader, NULL_CLASSLOADER_FILTER));

    URL resource = regionClassLoader.getResource(RESOURCE_NAME);
    Assert.assertThat(resource, CoreMatchers.equalTo(null));
  }

  @Test
  public void loadsResourceFromParentWhenIsNotDefinedInAnyRegionClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    List<ArtifactClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

    classLoaders.forEach(classLoader -> regionClassLoader.addClassLoader(classLoader, NULL_CLASSLOADER_FILTER));

    URL resource = regionClassLoader.getResource(RESOURCE_NAME);
    Assert.assertThat(resource, CoreMatchers.equalTo(PARENT_LOADED_RESOURCE));
  }

  @Test
  public void loadsResourceFromRegionMemberWhenIsDefinedInRegionAndParentClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    appClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
    regionClassLoader.addClassLoader(appClassLoader, new DefaultArtifactClassLoaderFilter(emptySet(), emptySet()));

    pluginClassLoader.addResource(RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(emptySet(), singleton(RESOURCE_NAME)));

    URL resource = regionClassLoader.getResource(RESOURCE_NAME);
    Assert.assertThat(resource, CoreMatchers.equalTo(PLUGIN_LOADED_RESOURCE));
  }

  @Test
  public void getsAllResources() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResources(RESOURCE_NAME)).thenReturn(new EnumerationAdapter<>(singleton(PARENT_LOADED_RESOURCE)));

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    appClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
    regionClassLoader.addClassLoader(appClassLoader, new DefaultArtifactClassLoaderFilter(emptySet(), singleton(RESOURCE_NAME)));

    pluginClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(emptySet(), singleton(RESOURCE_NAME)));

    final Enumeration<URL> resources = regionClassLoader.getResources(RESOURCE_NAME);

    List<URL> expectedResources = new LinkedList<>();
    expectedResources.add(APP_LOADED_RESOURCE);
    expectedResources.add(PLUGIN_LOADED_RESOURCE);
    expectedResources.add(PARENT_LOADED_RESOURCE);

    Assert.assertThat(resources, EnumerationMatcher.equalTo(expectedResources));
  }

  @Test
  public void disposesClassLoaders() throws Exception {
    when(lookupPolicy.getLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), lookupPolicy);

    final ArtifactClassLoader regionMember1 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);
    final ArtifactClassLoader regionMember2 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);
    regionClassLoader.addClassLoader(regionMember1, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember2, NULL_CLASSLOADER_FILTER);


    regionClassLoader.dispose();

    verify(regionMember1).dispose();
    verify(regionMember2).dispose();
  }

  @Test
  public void disposesClassLoadersEvenOnExceptions() throws Exception {
    when(lookupPolicy.getLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), lookupPolicy);

    final ArtifactClassLoader regionMember1 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);
    doThrow(new RuntimeException()).when(regionMember1).dispose();
    final ArtifactClassLoader regionMember2 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);

    regionClassLoader.addClassLoader(regionMember1, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember2, NULL_CLASSLOADER_FILTER);

    regionClassLoader.dispose();

    verify(regionMember1).dispose();
    verify(regionMember2).dispose();
  }

  private List<ArtifactClassLoader> getClassLoaders(ArtifactClassLoader... expectedClassLoaders) {
    List<ArtifactClassLoader> classLoaders = new LinkedList<>();

    Collections.addAll(classLoaders, expectedClassLoaders);

    return classLoaders;
  }

  public static class TestApplicationClassLoader extends TestArtifactClassLoader implements DisposableClassLoader {

    private boolean disposed;

    @Override
    public void dispose() {
      this.disposed = true;
    }
  }

  // Used to ensure that the composite classloader is able to access
  // protected methods in subclasses by reflection
  public static class SubTestClassLoader extends TestArtifactClassLoader {

  }
}
