/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.petstore.extension.PetStoreConnector;

import com.google.common.io.PatternFilenameFilter;

import java.io.File;
import java.util.Optional;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExtensionPluginMetadataGeneratorTestCase extends AbstractMuleTestCase {

  private static final String META_INF = "META-INF";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Artifact heisenbergPlugin = new DefaultArtifact("org.mule.tests:mule-heisenberg-extension:1.0-SNAPSHOT");
  private Artifact petStorePlugin = new DefaultArtifact("org.mule.tests:mule-petstore-extension:1.0-SNAPSHOT");

  private DependencyResolver depResolver;
  private ExtensionPluginMetadataGenerator generator;

  @Before
  public void before() throws Exception {
    depResolver = mock(DependencyResolver.class);
    ExtensionModelLoaderFinder finder = mock(ExtensionModelLoaderFinder.class);
    when(finder.findLoaderByProperty(anyObject(), anyObject(), anyObject()))
        .thenReturn(Optional.of(new DefaultJavaExtensionModelLoader()));
    generator = new ExtensionPluginMetadataGenerator(temporaryFolder.newFolder(), finder);
  }

  @Test
  public void scanningClassPathShouldNotIncludeSpringStuff() {
    Class scanned = generator.scanForExtensionAnnotatedClasses(heisenbergPlugin, newArrayList(this.getClass()
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()));

    assertThat(scanned, is(nullValue()));
  }

  @Test
  public void generateExtensionManifestForTwoExtensionsInDifferentFolders() {
    File heisenbergPluginFolder =
        generator.generateExtensionResources(heisenbergPlugin, HeisenbergExtension.class, depResolver, emptyList());
    File petStorePluginFolder =
        generator.generateExtensionResources(petStorePlugin, PetStoreConnector.class, depResolver, emptyList());
    assertThat(heisenbergPluginFolder, not(equalTo(petStorePluginFolder)));
  }

  @Test
  public void generateExtensionMetadataForTwoExtensionsInDifferentFolders() throws Exception {
    File heisenbergPluginFolder =
        generator.generateExtensionResources(heisenbergPlugin, HeisenbergExtension.class, depResolver, emptyList());
    File petStorePluginFolder =
        generator.generateExtensionResources(petStorePlugin, PetStoreConnector.class, depResolver, emptyList());

    assertThat(listFiles(heisenbergPluginFolder, "heisenberg.xsd"), arrayWithSize(0));
    assertThat(listFiles(heisenbergPluginFolder, "petstore.xsd"), arrayWithSize(0));

    assertThat(listFiles(petStorePluginFolder, "heisenberg.xsd"), arrayWithSize(0));
    assertThat(listFiles(petStorePluginFolder, "petstore.xsd"), arrayWithSize(0));
  }

  private String[] listFiles(File pluginResourcesFolder, String pattern) {
    return new File(pluginResourcesFolder, META_INF).list(new PatternFilenameFilter(pattern));
  }

}
