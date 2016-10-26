/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.util.FileUtils.stringToFile;
import static org.mule.runtime.deployment.model.internal.plugin.descriptor.PropertiesClassloaderDescriptor.PLUGINPROPERTIES;
import static org.mule.runtime.deployment.model.internal.plugin.loader.AbstractPluginDescriptorLoader.PLUGIN_JSON_DESCRIPTOR_FILE;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_PROPERTIES;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArtifactPluginDescriptorFactoryTestCase extends AbstractMuleTestCase {

  public static final String PLUGIN_NAME = "testPlugin";

  @Rule
  public TemporaryFolder pluginsFolder = new TemporaryFolder();

  private final ClassLoaderFilterFactory classLoaderFilterFactory = mock(ClassLoaderFilterFactory.class);
  private ArtifactPluginDescriptorFactory descriptorFactory = new ArtifactPluginDescriptorFactory(classLoaderFilterFactory);

  @Before
  public void setUp() throws Exception {
    when(classLoaderFilterFactory.create("", ""))
        .thenReturn(NULL_CLASSLOADER_FILTER);
  }

  @Test(expected = ArtifactDescriptorCreateException.class)
  public void parsesPluginWithNoDescriptor() throws Exception {
    final File pluginFolder = createPluginFolder();

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLoaderExportClass() throws Exception {
    final File pluginFolder = createPluginFolder();

    final String exportedClassPackages = "org.foo, org.bar";
    new PluginPropertiesBuilder(pluginFolder).exportingClassesFrom(exportedClassPackages).build();

    final ArtifactClassLoaderFilter classLoaderFilter = mock(DefaultArtifactClassLoaderFilter.class);
    when(classLoaderFilterFactory.create(exportedClassPackages, "")).thenReturn(classLoaderFilter);

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).limitingAccessWith(classLoaderFilter).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLoaderExportResource() throws Exception {
    final File pluginFolder = createPluginFolder();

    final String exportedResources = "META-INF, META-INF/xml";
    new PluginPropertiesBuilder(pluginFolder).exportingResourcesFrom(exportedResources).build();

    final ArtifactClassLoaderFilter classLoaderFilter = mock(DefaultArtifactClassLoaderFilter.class);
    when(classLoaderFilterFactory.create("", exportedResources)).thenReturn(classLoaderFilter);

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).limitingAccessWith(classLoaderFilter).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLibraries() throws Exception {
    final File pluginFolder = createPluginFolder();

    final File pluginLibFolder = new File(pluginFolder, "lib");
    assertThat(pluginLibFolder.mkdir(), is(true));
    new PluginPropertiesBuilder(pluginFolder).build();

    final File jar1 = createDummyJarFile(pluginLibFolder, "lib1.jar");
    final File jar2 = createDummyJarFile(pluginLibFolder, "lib2.jar");
    final URL[] libraries = new URL[] {jar1.toURI().toURL(), jar2.toURI().toURL()};

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).containing(libraries).assertPluginDescriptor(pluginDescriptor);
  }

  private File createPluginFolder() {
    final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
    assertThat(pluginFolder.mkdir(), is(true));
    return pluginFolder;
  }

  private File createDummyJarFile(File pluginLibFolder, String child) throws IOException {
    final File jar1 = new File(pluginLibFolder, child);
    FileUtils.write(jar1, "foo");
    return jar1;
  }

  private static class PluginDescriptorChecker {

    private final File pluginFolder;
    private URL[] runtimeLibs = new URL[0];;
    private ClassLoaderLookupPolicy classLoaderLookupPolicy = null;
    private ClassLoaderFilter classLoaderFilter = NULL_CLASSLOADER_FILTER;

    public PluginDescriptorChecker(File pluginFolder) {
      this.pluginFolder = pluginFolder;
    }

    public PluginDescriptorChecker limitingAccessWith(ClassLoaderFilter classLoaderFilter) {
      this.classLoaderFilter = classLoaderFilter;

      return this;
    }

    public PluginDescriptorChecker containing(URL[] libraries) {
      runtimeLibs = libraries;
      return this;
    }

    public PluginDescriptorChecker configuredWith(ClassLoaderLookupPolicy classLoaderLookupPolicy) {
      this.classLoaderLookupPolicy = classLoaderLookupPolicy;
      return this;
    }

    public void assertPluginDescriptor(ArtifactPluginDescriptor pluginDescriptor) {
      assertThat(pluginDescriptor.getName(), equalTo(pluginFolder.getName()));
      try {
        assertThat(pluginDescriptor.getRuntimeClassesDir(),
                   equalTo(new File(pluginFolder, "classes").toURI().toURL()));
      } catch (MalformedURLException e) {
        throw new AssertionError("Can't compare classes dir", e);
      }

      assertRuntimeLibs(pluginDescriptor);
      assertThat(pluginDescriptor.getClassLoaderFilter(), is(classLoaderFilter));
    }

    private void assertRuntimeLibs(ArtifactPluginDescriptor pluginDescriptor) {
      assertThat(pluginDescriptor.getRuntimeLibs().length, equalTo(runtimeLibs.length));
      for (URL libUrl : pluginDescriptor.getRuntimeLibs()) {
        assertThat(pluginDescriptor.getRuntimeLibs(), hasItemInArray(equalTo(libUrl)));
      }
    }
  }

  private static class PluginPropertiesBuilder {

    private final File pluginFolder;
    private String exportedClassPackages;
    private String exportedResources;

    public PluginPropertiesBuilder(File pluginFolder) {
      this.pluginFolder = pluginFolder;
    }

    public PluginPropertiesBuilder exportingClassesFrom(String packages) {
      this.exportedClassPackages = packages;

      return this;
    }

    public PluginPropertiesBuilder exportingResourcesFrom(String packages) {
      this.exportedResources = packages;

      return this;
    }

    public File build() throws IOException {
      final File pluginProperties = new File(pluginFolder, PLUGIN_PROPERTIES);
      if (pluginProperties.exists()) {
        throw new IllegalStateException(String.format("File '%s' already exists", pluginProperties.getAbsolutePath()));
      }

      pluginProperties.createNewFile();
      addDescriptorProperty(pluginProperties, EXPORTED_CLASS_PACKAGES_PROPERTY, this.exportedClassPackages);
      addDescriptorProperty(pluginProperties, EXPORTED_RESOURCE_PROPERTY, this.exportedResources);
      addJsonDescriptor();
      return pluginProperties;
    }

    private void addJsonDescriptor() {
      final File mulePluginJsonFile =
          new File(pluginFolder, PLUGIN_JSON_DESCRIPTOR_FILE.getPath());
      mulePluginJsonFile.deleteOnExit();

      MulePluginJsonFileBuilder.ClassloaderDescriptor classloaderDescriptor =
          new MulePluginJsonFileBuilder.ClassloaderDescriptor(PLUGINPROPERTIES);
      MulePluginJsonFileBuilder mulePluginJsonFileBuilder =
          new MulePluginJsonFileBuilder(pluginFolder.getName(), "4.0.0", classloaderDescriptor);
      String jsonContent = new Gson().toJson(mulePluginJsonFileBuilder);
      try {
        FileUtils.writeStringToFile(mulePluginJsonFile, jsonContent);
      } catch (IOException e) {
        throw new IllegalStateException(String.format("there was an issue generating the %s file",
                                                      PLUGIN_JSON_DESCRIPTOR_FILE.getPath()));
      }
    }

    private void addDescriptorProperty(File pluginProperties, String propertyName, String propertyValue) throws IOException {
      if (!StringUtils.isEmpty(propertyValue)) {
        final String descriptorProperty = generateDescriptorProperty(propertyName, propertyValue);

        stringToFile(pluginProperties.getAbsolutePath(), descriptorProperty, true);
      }
    }

    private String generateDescriptorProperty(String propertyName, String propertyValue) {
      StringBuilder builder = new StringBuilder(propertyName).append("=").append(propertyValue);

      return builder.toString();
    }
  }

  private static final class MulePluginJsonFileBuilder {

    //TODO MULE-10875 this class should be provided in mule-module-artifact, as well as a builder for it so it can be serialized in one place
    public final String name;
    public final String minMuleVersion;
    public final ClassloaderDescriptor classloaderDescriptor;

    public MulePluginJsonFileBuilder(String name, String minMuleVersion, ClassloaderDescriptor classloaderDescriptor) {
      this.name = name;
      this.minMuleVersion = minMuleVersion;
      this.classloaderDescriptor = classloaderDescriptor;
    }

    private static final class ClassloaderDescriptor {

      public final String id;

      public ClassloaderDescriptor(String id) {
        this.id = id;
      }
    }
  }
}
