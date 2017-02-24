/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.io.File.createTempFile;
import static java.util.Collections.emptySet;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.core.util.JarUtils.appendJarFileEntries;
import static org.mule.runtime.core.util.JarUtils.getUrlWithinJar;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_DEPENDENCIES;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_PROPERTIES;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.invalidBundleDescriptorLoaderIdError;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.invalidClassLoaderModelIdError;
import static org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader.FILE_SYSTEM_POLICY_MODEL_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.VERSION;
import static org.mule.tck.ZipUtils.compress;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.LoaderNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader;
import org.mule.tck.ZipUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ArtifactPluginDescriptorFactoryTestCase extends AbstractMuleTestCase {

  private static final String PLUGIN_NAME = "testPlugin.jar";
  private static final String INVALID_LOADER_ID = "INVALID";
  private static final String MIN_MULE_VERSION = "4.0.0";

  @Rule
  public TemporaryFolder pluginsTempFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final ClassLoaderFilterFactory classLoaderFilterFactory = mock(ClassLoaderFilterFactory.class);
  private final DescriptorLoaderRepository descriptorLoaderRepository = mock(DescriptorLoaderRepository.class);
  private ArtifactPluginDescriptorFactory descriptorFactory = new ArtifactPluginDescriptorFactory(descriptorLoaderRepository);

  @Before
  public void setUp() throws Exception {
    when(classLoaderFilterFactory.create(null, null))
        .thenReturn(NULL_CLASSLOADER_FILTER);

    when(descriptorLoaderRepository.get(FILE_SYSTEM_POLICY_MODEL_LOADER_ID, PLUGIN, ClassLoaderModelLoader.class))
        .thenReturn(new FileSystemPolicyClassLoaderModelLoader());
    when(descriptorLoaderRepository.get(INVALID_LOADER_ID, PLUGIN, ClassLoaderModelLoader.class))
        .thenThrow(new LoaderNotFoundException(INVALID_LOADER_ID));

    when(descriptorLoaderRepository.get(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, PLUGIN, BundleDescriptorLoader.class))
        .thenReturn(new PropertiesBundleDescriptorLoader());
    when(descriptorLoaderRepository.get(INVALID_LOADER_ID, PLUGIN, BundleDescriptorLoader.class))
        .thenThrow(new LoaderNotFoundException(INVALID_LOADER_ID));
  }

  @Test
  public void parsesPluginWithNoDescriptor() throws Exception {
    final File pluginFile = createPluginFile();

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFile);

    new PluginDescriptorChecker(pluginFile).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLoaderExportClass() throws Exception {
    final File pluginFile = createPluginFile();

    final String exportedClassPackages = "org.foo, org.bar";
    new PluginPropertiesBuilder(pluginFile).exportingClassesFrom(exportedClassPackages).build();

    final ArtifactClassLoaderFilter classLoaderFilter = mock(DefaultArtifactClassLoaderFilter.class);
    Set<String> parsedExportedPackages = new HashSet<>();
    parsedExportedPackages.add("org.foo");
    parsedExportedPackages.add("org.bar");
    when(classLoaderFilter.getExportedClassPackages()).thenReturn(parsedExportedPackages);
    when(classLoaderFilterFactory.create(exportedClassPackages, null)).thenReturn(classLoaderFilter);

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFile);

    new PluginDescriptorChecker(pluginFile).exportingPackages(parsedExportedPackages).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLoaderExportResource() throws Exception {
    final File pluginFile = createPluginFile();

    final String exportedResources = "META-INF, META-INF/xml";
    new PluginPropertiesBuilder(pluginFile).exportingResourcesFrom(exportedResources).build();

    final ArtifactClassLoaderFilter classLoaderFilter = mock(DefaultArtifactClassLoaderFilter.class);
    Set<String> parsedExportedResources = new HashSet<>();
    parsedExportedResources.add("META-INF");
    parsedExportedResources.add("META-INF/xml");
    when(classLoaderFilter.getExportedResources()).thenReturn(parsedExportedResources);
    when(classLoaderFilterFactory.create(null, exportedResources)).thenReturn(classLoaderFilter);

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFile);

    new PluginDescriptorChecker(pluginFile).exportingResources(parsedExportedResources)
        .assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLibraries() throws Exception {
    final File pluginTempLibFolder = pluginsTempFolder.newFolder("lib");
    final File jar1 = createDummyJarFile(pluginTempLibFolder, "lib1.jar");
    final File jar2 = createDummyJarFile(pluginTempLibFolder, "lib2.jar");
    final File pluginFile = createPluginFile(new ZipUtils.ZipResource[] {
        new ZipUtils.ZipResource(jar1.getAbsolutePath(), "lib/lib1.jar"),
        new ZipUtils.ZipResource(jar2.getAbsolutePath(), "lib/lib2.jar")
    });

    final URL[] libraries = new URL[] {getUrlWithinJar(pluginFile, "lib/lib1.jar"), getUrlWithinJar(pluginFile, "lib/lib2.jar")};

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFile);

    new PluginDescriptorChecker(pluginFile).containing(libraries).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesNamedDefinedPluginDependency() throws Exception {
    final File pluginFile = createPluginFile();

    final String pluginDependencies = "foo";
    new PluginPropertiesBuilder(pluginFile).dependingOn(pluginDependencies).build();

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFile);

    BundleDescriptor descriptor =
        new BundleDescriptor.Builder().setArtifactId("foo").setGroupId("test").setVersion("1.0").build();
    new PluginDescriptorChecker(pluginFile).dependingOn(descriptor).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesFullyDefinedPluginDependency() throws Exception {
    final File pluginFile = createPluginFile();

    final String pluginDependencies = "org.foo:foo:2.0";
    new PluginPropertiesBuilder(pluginFile).dependingOn(pluginDependencies).build();

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFile);

    BundleDescriptor descriptor =
        new BundleDescriptor.Builder().setArtifactId("foo").setGroupId("org.foo").setVersion("2.0").build();
    new PluginDescriptorChecker(pluginFile).dependingOn(descriptor).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesMultiplePluginDependencies() throws Exception {
    final File pluginFile = createPluginFile();

    final String pluginDependencies = "org.foo:foo:2.0,org.bar:bar:1.0";
    new PluginPropertiesBuilder(pluginFile).dependingOn(pluginDependencies).build();

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFile);

    BundleDescriptor fooDescriptor =
        new BundleDescriptor.Builder().setArtifactId("foo").setGroupId("org.foo").setVersion("2.0").build();
    BundleDescriptor barDEscriptor =
        new BundleDescriptor.Builder().setArtifactId("bar").setGroupId("org.bar").setVersion("1.0").build();
    new PluginDescriptorChecker(pluginFile).dependingOn(fooDescriptor).dependingOn(barDEscriptor)
        .assertPluginDescriptor(pluginDescriptor);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsTooParseDescriptorWithIncompletePluginDependency() throws Exception {
    final File pluginFile = createPluginFile();
    final String pluginDependencies = "org.foo:foo";
    new PluginPropertiesBuilder(pluginFile).dependingOn(pluginDependencies).build();

    descriptorFactory.create(pluginFile);
  }

  @Test
  public void detectsInvalidClassLoaderModelLoaderId() throws Exception {
    MulePluginModel.MulePluginModelBuilder pluginModelBuilder = new MulePluginModel.MulePluginModelBuilder().setName(PLUGIN_NAME)
        .setMinMuleVersion(MIN_MULE_VERSION)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));
    pluginModelBuilder.withClassLoaderModelDescriber().setId(INVALID_LOADER_ID);

    ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder(PLUGIN_NAME).tempFolder(pluginsTempFolder.newFolder())
            .describedBy(pluginModelBuilder.build());

    File pluginJarLocation = createJarFileFromArtifactFile(pluginFileBuilder);

    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException
        .expectMessage(invalidClassLoaderModelIdError(pluginJarLocation,
                                                      pluginModelBuilder.getClassLoaderModelDescriptorLoader()));

    descriptorFactory.create(pluginJarLocation);
  }

  private File createJarFileFromArtifactFile(ArtifactPluginFileBuilder pluginFileBuilder) throws IOException {
    // TODO MULE-11456 - Once we moved everything to .jar, we won't need this hack
    File artifactFile = pluginFileBuilder.getArtifactFile();
    File pluginJarLocation = new File(artifactFile.getParent(), artifactFile.getName().replace(".zip", ""));
    copyFile(artifactFile, pluginJarLocation);
    return pluginJarLocation;
  }

  @Test
  public void detectsInvalidBundleDescriptorModelLoaderId() throws Exception {
    MulePluginModel.MulePluginModelBuilder pluginModelBuilder = new MulePluginModel.MulePluginModelBuilder().setName(PLUGIN_NAME)
        .setMinMuleVersion(MIN_MULE_VERSION).withBundleDescriptorLoader(createBundleDescriptorLoader(INVALID_LOADER_ID));
    pluginModelBuilder.withClassLoaderModelDescriber().setId(FILE_SYSTEM_POLICY_MODEL_LOADER_ID);

    ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder(PLUGIN_NAME).tempFolder(pluginsTempFolder.newFolder())
            .describedBy(pluginModelBuilder.build());

    // TODO MULE-11456 - Once we moved everything to .jar, we won't need this hack
    File pluginJarLocation = createJarFileFromArtifactFile(pluginFileBuilder);

    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException
        .expectMessage(invalidBundleDescriptorLoaderIdError(pluginJarLocation, pluginModelBuilder.getBundleDescriptorLoader()));

    descriptorFactory.create(pluginJarLocation);
  }

  private File createPluginFile(ZipUtils.ZipResource... zipResources) {
    final File pluginFile = new File(pluginsTempFolder.getRoot(), PLUGIN_NAME);
    compress(pluginFile, zipResources);
    return pluginFile;
  }

  private File createDummyJarFile(File pluginLibFolder, String child) throws IOException {
    final File jar1 = new File(pluginLibFolder, child);
    FileUtils.write(jar1, "foo");
    return jar1;
  }

  private File createTempFolder() throws IOException {
    File tempFolder = createTempFile("tempPolicy", null);
    Assert.assertThat(tempFolder.delete(), Matchers.is(true));
    Assert.assertThat(tempFolder.mkdir(), Matchers.is(true));
    return tempFolder;
  }

  private MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String bundleDescriptorLoaderId) {
    Map<String, Object> attributes = new HashMap();
    attributes.put(VERSION, "1.0");
    attributes.put(GROUP_ID, "org.mule.test");
    attributes.put(ARTIFACT_ID, PLUGIN_NAME);
    attributes.put(CLASSIFIER, MULE_PLUGIN_CLASSIFIER);
    attributes.put(TYPE, "jar");
    return new MuleArtifactLoaderDescriptor(bundleDescriptorLoaderId, attributes);
  }

  private static class PluginDescriptorChecker {

    private final File pluginFile;
    private URL[] libraries = new URL[0];;
    private Set<String> resources = emptySet();
    private Set<String> packages = emptySet();
    private List<BundleDescriptor> dependencies = new ArrayList<>();

    public PluginDescriptorChecker(File pluginFile) {
      this.pluginFile = pluginFile;
    }

    public PluginDescriptorChecker exportingResources(Set<String> resources) {
      this.resources = resources;

      return this;
    }

    public PluginDescriptorChecker exportingPackages(Set<String> packages) {
      this.packages = packages;

      return this;
    }

    public PluginDescriptorChecker containing(URL[] libraries) {
      this.libraries = libraries;
      return this;
    }

    public PluginDescriptorChecker dependingOn(BundleDescriptor descriptor) {
      this.dependencies.add(descriptor);
      return this;
    }

    public void assertPluginDescriptor(ArtifactPluginDescriptor pluginDescriptor) throws Exception {
      assertThat(pluginDescriptor.getName(), equalTo(pluginFile.getName()));
      try {
        assertThat(pluginDescriptor.getClassLoaderModel().getUrls()[0],
                   equalTo(pluginFile.toURI().toURL()));
      } catch (MalformedURLException e) {
        throw new AssertionError("Can't compare classes dir", e);
      }

      assertUrls(pluginDescriptor);
      assertThat(pluginDescriptor.getClassLoaderModel().getExportedResources(), equalTo(resources));
      assertThat(pluginDescriptor.getClassLoaderModel().getExportedPackages(), equalTo(packages));
      assertPluginDependencies(pluginDescriptor.getClassLoaderModel());
    }

    private void assertPluginDependencies(ClassLoaderModel classLoaderModel) {
      assertThat(classLoaderModel.getDependencies().size(), equalTo(dependencies.size()));

      for (BundleDependency bundleDependency : classLoaderModel.getDependencies()) {
        assertThat(dependencies.contains(bundleDependency.getDescriptor()), is(true));
      }
    }

    private void assertUrls(ArtifactPluginDescriptor pluginDescriptor) throws Exception {
      assertThat(pluginDescriptor.getClassLoaderModel().getUrls().length, equalTo(libraries.length + 1));
      assertThat(pluginDescriptor.getClassLoaderModel().getUrls(),
                 hasItemInArray(equalTo(pluginFile.toURI().toURL())));

      for (URL libUrl : libraries) {
        assertThat(pluginDescriptor.getClassLoaderModel().getUrls(), hasItemInArray(equalTo(libUrl)));
      }
    }
  }

  private static class PluginPropertiesBuilder {

    private final File pluginFile;
    private String exportedClassPackages;
    private String exportedResources;
    private String pluginDependencies;

    public PluginPropertiesBuilder(File pluginFile) {
      this.pluginFile = pluginFile;
    }

    public PluginPropertiesBuilder exportingClassesFrom(String packages) {
      this.exportedClassPackages = packages;

      return this;
    }

    public PluginPropertiesBuilder exportingResourcesFrom(String packages) {
      this.exportedResources = packages;

      return this;
    }

    public PluginPropertiesBuilder dependingOn(String pluginDependencies) {
      this.pluginDependencies = pluginDependencies;

      return this;
    }

    public void build() throws Exception {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      addDescriptorProperty(byteArrayOutputStream, EXPORTED_CLASS_PACKAGES_PROPERTY, this.exportedClassPackages);
      addDescriptorProperty(byteArrayOutputStream, EXPORTED_RESOURCE_PROPERTY, this.exportedResources);
      if (!isEmpty(pluginDependencies)) {
        addDescriptorProperty(byteArrayOutputStream, PLUGIN_DEPENDENCIES, this.pluginDependencies);
      }
      LinkedHashMap entries = new LinkedHashMap();
      entries.put(PLUGIN_PROPERTIES, byteArrayOutputStream.toByteArray());
      appendJarFileEntries(pluginFile, entries);
    }

    private void addDescriptorProperty(ByteArrayOutputStream byteArrayOutputStream, String propertyName, String propertyValue)
        throws IOException {
      if (!isEmpty(propertyValue)) {
        byteArrayOutputStream.write((propertyName + "=" + propertyValue).getBytes());
      }
    }

  }
}
