/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.CONTAINER_APP_PLUGINS;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.tck.ZipUtils;
import org.mule.tck.ZipUtils.ZipResource;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArtifactPluginRepositoryTestCase extends AbstractMuleTestCase {

  private static final String PLUGIN_NAME = "testPlugin";

  private static final String PLUGIN_PROPERTIES = "plugin.properties";
  private static final String PLUGIN_LIB_FOLDER = "lib";

  @Rule
  public TemporaryFolder muleHomeFolder = new TemporaryFolder();

  private final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory =
      mock(ArtifactPluginDescriptorFactory.class);
  private final ArtifactPluginRepository applicationPluginRepository =
      new DefaultArtifactPluginRepository(artifactPluginDescriptorFactory);

  private File pluginsLibFolder;

  @Before
  public void setUp() throws IOException {
    System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, muleHomeFolder.getRoot().getCanonicalPath());
    when(artifactPluginDescriptorFactory.create(anyObject()))
        .thenAnswer(invocation -> new ArtifactPluginDescriptor(((File) invocation.getArguments()[0]).getName()));

    pluginsLibFolder = createContainerAppPluginsFolder();
  }

  @Test
  public void emptyListOfPlugins() throws Exception {
    final List<ArtifactPluginDescriptor> descriptorList = applicationPluginRepository.getContainerArtifactPluginDescriptors();
    assertThat(descriptorList.size(), is(0));
  }

  @Test
  public void unzipPluginZipFileCreateDescriptor() throws Exception {
    File zipPlugin = createPluginZipFile(pluginsLibFolder, PLUGIN_NAME);

    final List<ArtifactPluginDescriptor> descriptorList = applicationPluginRepository.getContainerArtifactPluginDescriptors();

    assertThat(descriptorList.size(), is(1));
    ArtifactPluginDescriptor descriptor = descriptorList.get(0);
    assertThat(descriptor.getName(), is(PLUGIN_NAME));

    assertThat(zipPlugin.exists(), is(false));
    assertThat(new File(pluginsLibFolder, descriptor.getName()).exists(), is(true));
  }

  @Test
  public void loadPluginAlreadyUnzippedCreateDescriptor() throws Exception {
    File pluginFolder = createPluginFolder(pluginsLibFolder, PLUGIN_NAME);

    final List<ArtifactPluginDescriptor> descriptorList = applicationPluginRepository.getContainerArtifactPluginDescriptors();

    assertThat(descriptorList.size(), is(1));
    ArtifactPluginDescriptor descriptor = descriptorList.get(0);
    assertThat(descriptor.getName(), is(PLUGIN_NAME));

    assertThat(pluginFolder.exists(), is(true));
  }

  private File createContainerAppPluginsFolder() throws IOException {
    final File pluginsFolder = new File(muleHomeFolder.getRoot(), CONTAINER_APP_PLUGINS);
    assertThat(pluginsFolder.mkdir(), is(true));
    return pluginsFolder;
  }

  private File createPluginZipFile(File pluginsLibFolder, String pluginName) throws IOException {
    final File pluginFolder = new File(pluginsLibFolder, pluginName);
    final File pluginPropertiesFile = new File(pluginFolder, PLUGIN_PROPERTIES);
    FileUtils.write(pluginPropertiesFile, "foo");
    final File libFolder = new File(pluginFolder, PLUGIN_LIB_FOLDER);
    final String libraryJarName = "library.jar";
    final File dummyJar = new File(libFolder, libraryJarName);
    FileUtils.write(dummyJar, "bar");


    File zipFile = new File(pluginsLibFolder, pluginName + ".zip");
    ZipUtils.compress(zipFile,
                      new ZipResource[] {
                          new ZipResource(dummyJar.getAbsolutePath(), PLUGIN_LIB_FOLDER + separator + libraryJarName),
                          new ZipResource(pluginPropertiesFile.getAbsolutePath(), pluginPropertiesFile.getName())});

    FileUtils.forceDelete(pluginFolder);

    return zipFile;
  }

  private File createPluginFolder(File pluginsLibFolder, String pluginName) throws IOException {
    final File pluginZipFile = createPluginZipFile(pluginsLibFolder, pluginName);
    final File pluginFolder = new File(pluginsLibFolder, pluginName);
    FileUtils.unzip(pluginZipFile, pluginFolder);
    FileUtils.forceDelete(pluginZipFile);
    return pluginFolder;
  }
}
