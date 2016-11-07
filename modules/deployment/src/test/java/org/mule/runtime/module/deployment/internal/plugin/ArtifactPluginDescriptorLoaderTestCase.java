/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.plugin;

import static java.io.File.separator;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.artifact.classloader.net.MuleArtifactUrlConnection.SEPARATOR;
import static org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler.PROTOCOL;
import static org.mule.runtime.core.util.FileUtils.stringToFile;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.deployment.internal.plugin.AbstractArtifactPluginDescriptorLoader.LIB;
import static org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_PROPERTIES;
import static org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorZipLoader.CLASSES;
import static org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorZipLoader.EXTENSION_ZIP;
import static org.mule.tck.ZipUtils.compress;
import org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler;
import org.mule.module.artifact.classloader.net.MuleUrlStreamHandlerFactory;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.tck.ZipUtils.ZipResource;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test all subclasses of {@link org.mule.runtime.module.deployment.internal.plugin.AbstractArtifactPluginDescriptorLoader}
 *
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class ArtifactPluginDescriptorLoaderTestCase extends AbstractMuleTestCase {

  static {
    MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    MuleArtifactUrlStreamHandler.register();
  }

  private static final String PLUGIN_NAME = "testPlugin";

  @Rule
  public TemporaryFolder pluginsFolder = new TemporaryFolder();

  @Parameterized.Parameter
  public boolean shouldCompressPluginFolder;

  @Parameterized.Parameters(name = "Should compress plugin folder: {0}")
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {false},
        {true}
    });
  }

  @Test
  public void parsesPluginWithNoDescriptor() throws Exception {
    final File pluginFolder = createPluginFolder();

    final ArtifactPluginDescriptor pluginDescriptor = load(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLoaderExportClass() throws Exception {
    final File pluginFolder = createPluginFolder();

    final String exportedClassPackages = "org.foo, org.bar";
    new PluginPropertiesBuilder(pluginFolder).exportingClassesFrom(exportedClassPackages).build();

    final ArtifactClassLoaderFilter classLoaderFilter = mock(DefaultArtifactClassLoaderFilter.class);
    Set<String> parsedExportedPackages = new HashSet<>();
    parsedExportedPackages.add("org.foo");
    parsedExportedPackages.add("org.bar");
    when(classLoaderFilter.getExportedClassPackages()).thenReturn(parsedExportedPackages);

    final ArtifactPluginDescriptor pluginDescriptor = load(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).exportingPackages(parsedExportedPackages).assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLoaderExportResource() throws Exception {
    final File pluginFolder = createPluginFolder();

    final String exportedResources = "META-INF, META-INF/xml";
    new PluginPropertiesBuilder(pluginFolder).exportingResourcesFrom(exportedResources).build();

    final ArtifactClassLoaderFilter classLoaderFilter = mock(DefaultArtifactClassLoaderFilter.class);
    Set<String> parsedExportedResources = new HashSet<>();
    parsedExportedResources.add("META-INF");
    parsedExportedResources.add("META-INF/xml");
    when(classLoaderFilter.getExportedResources()).thenReturn(parsedExportedResources);

    final ArtifactPluginDescriptor pluginDescriptor = load(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).exportingResources(parsedExportedResources)
        .assertPluginDescriptor(pluginDescriptor);
  }

  @Test
  public void parsesLibraries() throws Exception {
    final File pluginFolder = createPluginFolder();

    final File pluginLibFolder = new File(pluginFolder, "lib");
    MatcherAssert.assertThat(pluginLibFolder.mkdir(), is(true));

    final File jar1 = createDummyJarFile(pluginLibFolder, "lib1.jar");
    final File jar2 = createDummyJarFile(pluginLibFolder, "lib2.jar");
    final File[] libraries = new File[] {jar1, jar2};

    final ArtifactPluginDescriptor pluginDescriptor = load(pluginFolder);

    new PluginDescriptorChecker(pluginFolder).containing(libraries).assertPluginDescriptor(pluginDescriptor);
  }

  private File createPluginFolder() {
    final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
    MatcherAssert.assertThat(pluginFolder.mkdir(), is(true));
    return pluginFolder;
  }

  private File createDummyJarFile(File pluginLibFolder, String child) throws IOException {
    final File jar1 = new File(pluginLibFolder, child);
    FileUtils.write(jar1, "foo");
    return jar1;
  }

  private ArtifactPluginDescriptor load(File pluginLocation) {
    if (shouldCompressPluginFolder) {
      File targetFile = new File(pluginLocation.getAbsolutePath() + EXTENSION_ZIP);

      compressDirectory(pluginLocation.toPath(), targetFile);
      return new ArtifactPluginDescriptorZipLoader(targetFile).load();
    } else {
      return new ArtifactPluginDescriptorFolderLoader(pluginLocation).load();
    }
  }

  private static void compressDirectory(Path path, File targetFile) {
    List<ZipResource> resources = new ArrayList<>();
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          resources.add(new ZipResource(file.toString(),
                                        StringUtils.removeStart(file.toString(), path.toString() + separator)));
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    compress(targetFile, resources.toArray(new ZipResource[resources.size()]));
  }

  private class PluginDescriptorChecker {

    private final File pluginFolder;
    private File[] libraries = new File[0];
    private Set<String> resources = emptySet();
    private Set<String> packages = emptySet();

    public PluginDescriptorChecker(File pluginFolder) {
      if (shouldCompressPluginFolder) {
        pluginFolder = new File(pluginFolder.getAbsolutePath() + EXTENSION_ZIP);
      }
      this.pluginFolder = pluginFolder;
    }

    public PluginDescriptorChecker exportingResources(Set<String> resources) {
      this.resources = resources;

      return this;
    }

    public PluginDescriptorChecker exportingPackages(Set<String> packages) {
      this.packages = packages;

      return this;
    }

    public PluginDescriptorChecker containing(File[] libraries) {
      this.libraries = libraries;
      return this;
    }

    public void assertPluginDescriptor(ArtifactPluginDescriptor pluginDescriptor) throws Exception {
      MatcherAssert.assertThat(pluginDescriptor.getName(), equalTo(getName()));
      try {
        MatcherAssert.assertThat(pluginDescriptor.getClassLoaderModel().getUrls()[0],
                                 equalTo(getClasses()));
      } catch (MalformedURLException e) {
        throw new AssertionError("Can't compare classes dir", e);
      }

      assertUrls(pluginDescriptor);
      MatcherAssert.assertThat(pluginDescriptor.getRootFolder(), equalTo(pluginFolder));
      MatcherAssert.assertThat(pluginDescriptor.getClassLoaderModel().getExportedResources(), equalTo(resources));
      MatcherAssert.assertThat(pluginDescriptor.getClassLoaderModel().getExportedPackages(), equalTo(packages));
    }

    protected String getName() {
      if (shouldCompressPluginFolder) {
        return pluginFolder.getName().replace(EXTENSION_ZIP, "");
      } else {
        return pluginFolder.getName();
      }
    }

    private URL getClasses() throws MalformedURLException {
      if (shouldCompressPluginFolder) {
        return new URL(PROTOCOL + ":" + pluginFolder.toURI() + SEPARATOR + CLASSES + SEPARATOR);
      } else {
        return new File(pluginFolder, CLASSES).toURI().toURL();
      }
    }

    private void assertUrls(ArtifactPluginDescriptor pluginDescriptor) throws Exception {
      MatcherAssert.assertThat(pluginDescriptor.getClassLoaderModel().getUrls().length, equalTo(libraries.length + 1));
      MatcherAssert.assertThat(pluginDescriptor.getClassLoaderModel().getUrls(),
                               hasItemInArray(equalTo(getClasses())));

      for (File libFile : libraries) {
        MatcherAssert.assertThat(pluginDescriptor.getClassLoaderModel().getUrls(), hasItemInArray(equalTo(getLibFile(libFile))));
      }
    }

    private URL getLibFile(File libFile) throws MalformedURLException {
      if (shouldCompressPluginFolder) {
        return new URL(PROTOCOL + ":" + pluginFolder.toURI() + SEPARATOR + LIB + "/" + libFile.getName() + SEPARATOR);
      } else {
        return libFile.toURI().toURL();
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

      addDescriptorProperty(pluginProperties, EXPORTED_CLASS_PACKAGES_PROPERTY, this.exportedClassPackages);
      addDescriptorProperty(pluginProperties, EXPORTED_RESOURCE_PROPERTY, this.exportedResources);

      return pluginProperties;
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
}
