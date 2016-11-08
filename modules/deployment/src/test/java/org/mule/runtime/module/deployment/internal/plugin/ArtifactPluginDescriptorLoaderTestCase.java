/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.plugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.util.FileUtils.createFile;
import static org.mule.tck.ZipUtils.compress;

import org.mule.tck.ZipUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;


public class ArtifactPluginDescriptorLoaderTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder pluginsFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory = mock(ArtifactPluginDescriptorFactory.class);

  private ArtifactPluginDescriptorLoader pluginDescriptorLoader;

  @Before
  public void createClasUnderTest() {
    pluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
  }

  @Test
  public void nullPluginZip() throws IOException {
    expectedException.expect(IllegalArgumentException.class);
    pluginDescriptorLoader.load(null, pluginsFolder.getRoot());
  }

  @Test
  public void nullUnpackDestination() throws IOException {
    expectedException.expect(IllegalArgumentException.class);
    pluginDescriptorLoader.load(pluginsFolder.getRoot(), null);
  }

  @Test
  public void loadAFileNonZipFile() throws IOException {
    expectedException.expect(IllegalArgumentException.class);
    File nonZipFile = createFile(new File(pluginsFolder.getRoot(), "test").getAbsolutePath());
    pluginDescriptorLoader.load(nonZipFile, pluginsFolder.getRoot());
  }

  @Test
  public void load() throws Exception {
    String pluginName = "plugin";
    File plugin = pluginsFolder.newFile(pluginName + ".zip");
    compress(plugin, new ZipUtils.ZipResource[] {});
    File unpackDestination = pluginsFolder.newFolder("destination");
    pluginDescriptorLoader.load(plugin, unpackDestination);
    verify(artifactPluginDescriptorFactory).create(new File(unpackDestination, pluginName));
  }

}
