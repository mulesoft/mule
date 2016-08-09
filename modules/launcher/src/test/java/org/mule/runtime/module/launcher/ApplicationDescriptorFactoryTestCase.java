/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppPluginsFolder;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.launcher.application.DuplicateExportedPackageException;
import org.mule.runtime.module.launcher.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationDescriptorFactoryTestCase extends AbstractMuleTestCase {

  public static final String APP_NAME = "testApp";
  public static final String JAR_FILE_NAME = "test.jar";

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
  private ArtifactPluginRepository applicationPluginRepository;

  @Before
  public void setUp() throws Exception {
    applicationPluginRepository = mock(ArtifactPluginRepository.class);
    when(applicationPluginRepository.getContainerArtifactPluginDescriptors()).thenReturn(emptyList());
  }

  @Test
  public void readsPlugin() throws Exception {
    File pluginDir = getAppPluginsFolder(APP_NAME);
    pluginDir.mkdirs();
    final File pluginFile = new ArtifactPluginFileBuilder("plugin").usingLibrary("lib/echo-test.jar").getArtifactFile();
    copyFile(pluginFile, new File(pluginDir, "plugin1.zip"));
    copyFile(pluginFile, new File(pluginDir, "plugin2.zip"));

    final ArtifactPluginDescriptorFactory pluginDescriptorFactory = mock(ArtifactPluginDescriptorFactory.class);

    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(pluginDescriptorFactory),
                                         applicationPluginRepository);
    final ArtifactPluginDescriptor expectedPluginDescriptor1 = mock(ArtifactPluginDescriptor.class);
    when(expectedPluginDescriptor1.getName()).thenReturn("plugin1");
    when(expectedPluginDescriptor1.getClassLoaderFilter()).thenReturn(ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER);
    final ArtifactPluginDescriptor expectedPluginDescriptor2 = mock(ArtifactPluginDescriptor.class);
    when(expectedPluginDescriptor2.getName()).thenReturn("plugin2");
    when(expectedPluginDescriptor2.getClassLoaderFilter()).thenReturn(ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER);
    when(pluginDescriptorFactory.create(any())).thenReturn(expectedPluginDescriptor1).thenReturn(expectedPluginDescriptor2);

    ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

    Set<ArtifactPluginDescriptor> plugins = desc.getPlugins();
    assertThat(plugins.size(), equalTo(2));
    assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor1)));
    assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor2)));
  }

  @Test
  public void readsSharedPluginLibs() throws Exception {
    File pluginLibDir = MuleFoldersUtil.getAppSharedPluginLibsFolder(APP_NAME);
    pluginLibDir.mkdirs();

    copyResourceAs("test-jar-with-resources.jar", pluginLibDir, JAR_FILE_NAME);
    ApplicationDescriptor desc =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory(new DefaultArtifactClassLoaderFilterFactory())),
                                         applicationPluginRepository).create(getAppFolder(APP_NAME));

    File sharedPluginFolder = desc.getSharedPluginFolder();

    assertThat(sharedPluginFolder.getAbsolutePath(), is(pluginLibDir.getAbsolutePath()));
  }

  @Test
  @Ignore("MULE-9649")
  public void validatesExportedPackageDuplication() throws Exception {
    File pluginDir = getAppPluginsFolder(APP_NAME);
    pluginDir.mkdirs();

    final File pluginFile = createApplicationPluginFile();
    copyFile(pluginFile, new File(pluginDir, "plugin1.zip"));
    copyFile(pluginFile, new File(pluginDir, "plugin2.zip"));

    doPackageValidationTest(applicationPluginRepository);
  }

  @Test
  @Ignore("MULE-9649")
  public void validatesExportedPackageDuplicationAgainstContainerPlugin() throws Exception {
    File pluginDir = getAppPluginsFolder(APP_NAME);
    pluginDir.mkdirs();
    copyFile(createApplicationPluginFile(), new File(pluginDir, "plugin1.zip"));

    final ArtifactPluginRepository applicationPluginRepository = mock(ArtifactPluginRepository.class);
    final ArtifactPluginDescriptor plugin2Descriptor = new ArtifactPluginDescriptor();
    plugin2Descriptor.setName("plugin2");
    final Set<String> exportedPackages = new HashSet<>();
    exportedPackages.add("org.foo");
    exportedPackages.add("org.bar");
    plugin2Descriptor.setClassLoaderFilter(new ArtifactClassLoaderFilter(exportedPackages, Collections.emptySet()));
    when(applicationPluginRepository.getContainerArtifactPluginDescriptors())
        .thenReturn(Collections.singletonList(plugin2Descriptor));

    doPackageValidationTest(applicationPluginRepository);
  }

  private File createApplicationPluginFile() throws Exception {
    return new ArtifactPluginFileBuilder("plugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo, org.bar")
        .getArtifactFile();
  }

  private void doPackageValidationTest(ArtifactPluginRepository applicationPluginRepository) {
    final ArtifactPluginDescriptorFactory pluginDescriptorFactory =
        new ArtifactPluginDescriptorFactory(new DefaultArtifactClassLoaderFilterFactory());
    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(pluginDescriptorFactory),
                                         applicationPluginRepository);

    try {
      applicationDescriptorFactory.create(getAppFolder(APP_NAME));
      fail("Descriptor creation was supposed to fail as the same packages are exported by two plugins");
    } catch (DuplicateExportedPackageException e) {
      assertThat(e.getMessage(), containsString("Package org.foo is exported on artifacts: plugin1, plugin2"));
      assertThat(e.getMessage(), containsString("Package org.bar is exported on artifacts: plugin1, plugin2"));
    }
  }

  private void copyResourceAs(String resourceName, File folder, String fileName) throws IOException {
    final InputStream sourcePlugin = IOUtils.getResourceAsStream(resourceName, getClass());
    IOUtils.copy(sourcePlugin, new FileOutputStream(new File(folder, fileName)));
  }
}
