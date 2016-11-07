/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.module.deployment.internal.plugin.ZipArtifactPluginDescriptorLoader.EXTENSION_ZIP;
import static org.mule.tck.ZipUtils.compress;
import org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler;
import org.mule.module.artifact.classloader.net.MuleUrlStreamHandlerFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.tck.ZipUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArtifactPluginDescriptorFactoryTestCase extends AbstractMuleTestCase {

  static {
    MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    MuleArtifactUrlStreamHandler.register();
  }

  private static final String PLUGIN_NAME = "testPlugin";

  @Rule
  public TemporaryFolder pluginsFolder = new TemporaryFolder();

  private final ClassLoaderFilterFactory classLoaderFilterFactory = mock(ClassLoaderFilterFactory.class);
  private ArtifactPluginDescriptorFactory descriptorFactory = new ArtifactPluginDescriptorFactory(classLoaderFilterFactory);

  @Test
  public void readZipPlugin() throws Exception {
    final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
    assertThat(pluginFolder.mkdir(), is(true));

    final File compressedFile = new File(pluginFolder, PLUGIN_NAME + EXTENSION_ZIP);
    compress(compressedFile, new ZipUtils.ZipResource[] {});
    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(compressedFile);

    assertEmptyArtifactPluginDescriptor(pluginDescriptor, compressedFile);
  }

  @Test
  public void readFolderPlugin() throws Exception {
    final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
    assertThat(pluginFolder.mkdir(), is(true));

    final ArtifactPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

    assertEmptyArtifactPluginDescriptor(pluginDescriptor, pluginFolder);
  }

  @Test(expected = ArtifactDescriptorCreateException.class)
  public void readNotSupportedFormatPlugin() throws Exception {
    final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
    assertThat(pluginFolder.mkdir(), is(true));

    final File compressedFile = new File(pluginFolder, PLUGIN_NAME + ".tar.gz");
    compress(compressedFile, new ZipUtils.ZipResource[] {});
    descriptorFactory.create(compressedFile);
  }

  private void assertEmptyArtifactPluginDescriptor(ArtifactPluginDescriptor pluginDescriptor, File expectedRootFolder) {
    assertThat(pluginDescriptor.getName(), is(PLUGIN_NAME));
    assertThat(pluginDescriptor.getRootFolder(), is(expectedRootFolder));
    assertThat(pluginDescriptor.getClassLoaderModel().getUrls().length, is(1));
    assertThat(pluginDescriptor.getClassLoaderModel().getExportedPackages().size(), is(0));
    assertThat(pluginDescriptor.getClassLoaderModel().getExportedResources().size(), is(0));
  }
}
