/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.RegionClassLoader.REGION_OWNER_CANNOT_BE_REMOVED_ERROR;
import static org.mule.runtime.module.artifact.api.classloader.RegionClassLoader.createCannotRemoveClassLoaderError;
import static org.mule.runtime.module.artifact.api.classloader.RegionClassLoader.createClassLoaderAlreadyInRegionError;
import static org.mule.runtime.module.artifact.api.classloader.RegionClassLoader.duplicatePackageMappingError;
import static org.mule.runtime.module.artifact.api.classloader.RegionClassLoader.illegalPackageMappingError;
import org.mule.runtime.core.internal.util.EnumerationAdapter;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.util.EnumerationMatcher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RegionClassLoaderTestCase extends AbstractMuleTestCase {

  private static final String PACKAGE_NAME = "java.lang";
  private static final String CLASS_NAME = PACKAGE_NAME + ".Object";
  private static final Class PARENT_LOADED_CLASS = Object.class;
  private static final Class PLUGIN_LOADED_CLASS = String.class;
  private static final String RESOURCE_NAME = "dummy.txt";
  public static final String APP_NAME = "testApp";
  private static final String ARTIFACT_ID = "testAppId";

  private final URL APP_LOADED_RESOURCE;
  private final URL PLUGIN_LOADED_RESOURCE;
  private final URL PARENT_LOADED_RESOURCE;

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
  private final ArtifactDescriptor artifactDescriptor;
  private TestApplicationClassLoader appClassLoader;
  private TestArtifactClassLoader pluginClassLoader;

  @Rule
  public ExpectedException expectedException = none();

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
    List<ArtifactClassLoader> classLoaders = createClassLoaders(regionClassLoader);

    classLoaders.forEach(classLoader -> regionClassLoader.addClassLoader(classLoader, NULL_CLASSLOADER_FILTER));

    when(lookupPolicy.getClassLookupStrategy(Object.class.getName())).thenReturn(CHILD_FIRST);
    regionClassLoader.loadClass(CLASS_NAME);
  }

  private List<ArtifactClassLoader> createClassLoaders(ClassLoader parent) {
    appClassLoader = new TestApplicationClassLoader(parent);
    pluginClassLoader = new SubTestClassLoader(parent);

    List<ArtifactClassLoader> classLoaders = new LinkedList<>();
    Collections.addAll(classLoaders, appClassLoader, pluginClassLoader);
    return classLoaders;
  }


  @Test
  public void loadsParentClassWhenIsNotDefinedInAnyRegionClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.loadClass(CLASS_NAME)).thenReturn(PARENT_LOADED_CLASS);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    List<ArtifactClassLoader> classLoaders = createClassLoaders(regionClassLoader);

    classLoaders.forEach(classLoader -> regionClassLoader.addClassLoader(classLoader, NULL_CLASSLOADER_FILTER));
    when(lookupPolicy.getClassLookupStrategy(Object.class.getName())).thenReturn(CHILD_FIRST);
    final Class loadedClass = regionClassLoader.loadClass(CLASS_NAME);
    assertThat(loadedClass, equalTo(PARENT_LOADED_CLASS));
  }

  @Test
  public void loadsClassFromRegionMemberWhenPackageMappingDefined() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.loadClass(CLASS_NAME)).thenReturn(PARENT_LOADED_CLASS);

    when(lookupPolicy.getClassLookupStrategy(Object.class.getName())).thenReturn(CHILD_FIRST);
    when(lookupPolicy.getPackageLookupStrategy(PACKAGE_NAME)).thenReturn(CHILD_FIRST);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);

    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(singleton(PACKAGE_NAME), emptySet()));
    pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);
    final Class loadedClass = regionClassLoader.loadClass(CLASS_NAME);
    assertThat(loadedClass, equalTo(PLUGIN_LOADED_CLASS));
  }

  @Test
  public void returnsNullResourceWhenIsNotDefinedInAnyClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(null);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    List<ArtifactClassLoader> classLoaders = createClassLoaders(regionClassLoader);

    classLoaders.forEach(classLoader -> regionClassLoader.addClassLoader(classLoader, NULL_CLASSLOADER_FILTER));

    URL resource = regionClassLoader.getResource(RESOURCE_NAME);
    Assert.assertThat(resource, CoreMatchers.equalTo(null));
  }

  @Test
  public void loadsResourceFromParentWhenIsNotDefinedInAnyRegionClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(emptySet(), singleton(RESOURCE_NAME)));

    URL resource = regionClassLoader.getResource(RESOURCE_NAME);
    Assert.assertThat(resource, CoreMatchers.equalTo(PARENT_LOADED_RESOURCE));
  }

  @Test
  public void loadsResourceFromRegionMemberWhenIsDefinedInRegionAndParentClassLoader() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);

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
    createClassLoaders(regionClassLoader);

    regionClassLoader.addClassLoader(appClassLoader, new DefaultArtifactClassLoaderFilter(emptySet(), singleton(RESOURCE_NAME)));
    appClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
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
    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), lookupPolicy);
    createClassLoaders(regionClassLoader);

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
    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), lookupPolicy);
    createClassLoaders(regionClassLoader);

    final ArtifactClassLoader regionMember1 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);
    doThrow(new RuntimeException()).when(regionMember1).dispose();
    final ArtifactClassLoader regionMember2 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);

    regionClassLoader.addClassLoader(regionMember1, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember2, NULL_CLASSLOADER_FILTER);

    regionClassLoader.dispose();

    verify(regionMember1).dispose();
    verify(regionMember2).dispose();
  }

  @Test
  public void verifiesThatClassLoaderIsNotAddedTwice() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);

    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(createClassLoaderAlreadyInRegionError(appClassLoader.getArtifactId()));
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
  }

  @Test
  public void failsToAddClassLoaderThatOverridesPackageMapping() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(lookupPolicy.getPackageLookupStrategy(anyString())).thenReturn(CHILD_FIRST);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);

    regionClassLoader.addClassLoader(appClassLoader, new DefaultArtifactClassLoaderFilter(singleton(PACKAGE_NAME), emptySet()));
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(duplicatePackageMappingError(PACKAGE_NAME, appClassLoader, pluginClassLoader));
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(singleton(PACKAGE_NAME), emptySet()));
  }

  @Test
  public void failsToAddClassLoaderThatOverridesLookupPolicy() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(lookupPolicy.getPackageLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(illegalPackageMappingError(PACKAGE_NAME, PARENT_FIRST));
    regionClassLoader.addClassLoader(appClassLoader, new DefaultArtifactClassLoaderFilter(singleton(PACKAGE_NAME), emptySet()));
  }

  @Test
  public void doesNotRemoveClassLoaderIfNotARegionMember() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);

    assertThat(regionClassLoader.removeClassLoader(appClassLoader), is(false));
  }

  @Test
  public void removesClassLoaderMember() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader, NULL_CLASSLOADER_FILTER);

    assertThat(regionClassLoader.removeClassLoader(pluginClassLoader), is(true));
    assertThat(regionClassLoader.getArtifactPluginClassLoaders(), is(empty()));
  }

  @Test
  public void failsToRemoveRegionOwner() throws Exception {

    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(REGION_OWNER_CANNOT_BE_REMOVED_ERROR);
    regionClassLoader.removeClassLoader(appClassLoader);
  }

  @Test
  public void failsToRemoveRegionMemberIfExportsPackages() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);
    when(lookupPolicy.getPackageLookupStrategy(anyString())).thenReturn(CHILD_FIRST);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader, new DefaultArtifactClassLoaderFilter(singleton("org.foo"), emptySet()));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(createCannotRemoveClassLoaderError(appClassLoader.getArtifactId()));
    regionClassLoader.removeClassLoader(pluginClassLoader);
  }

  @Test
  public void failsToRemoveRegionMemberIfExportsResources() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.getResource(RESOURCE_NAME)).thenReturn(PARENT_LOADED_RESOURCE);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(emptySet(), singleton("META-INF/pom.xml")));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(createCannotRemoveClassLoaderError(appClassLoader.getArtifactId()));
    regionClassLoader.removeClassLoader(pluginClassLoader);
  }

  @Test
  public void getsPluginsClassLoaders() throws Exception {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader, NULL_CLASSLOADER_FILTER);

    assertThat(regionClassLoader.getArtifactPluginClassLoaders(), contains(pluginClassLoader));
  }

  public static class TestApplicationClassLoader extends TestArtifactClassLoader {

    public TestApplicationClassLoader(ClassLoader parent) {
      super(parent);
    }
  }

  // Used to ensure that the composite classloader is able to access
  // protected methods in subclasses by reflection
  public static class SubTestClassLoader extends TestArtifactClassLoader {

    SubTestClassLoader(ClassLoader parent) {
      super(parent);
    }
  }
}
