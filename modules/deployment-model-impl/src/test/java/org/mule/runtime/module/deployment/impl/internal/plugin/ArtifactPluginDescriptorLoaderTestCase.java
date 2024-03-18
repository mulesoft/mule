/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.util.Optional.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.tck.ZipUtils.compress;

import org.mule.tck.ZipUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;

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

  private final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory = mock(ArtifactPluginDescriptorFactory.class);

  private ArtifactPluginDescriptorLoader pluginDescriptorLoader;

  @Before
  public void createClasUnderTest() {
    pluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
  }

  @Test
  public void load() throws Exception {
    String pluginName = "plugin";
    File plugin = pluginsFolder.newFile(pluginName + ".jar");
    compress(plugin, new ZipUtils.ZipResource[] {});
    pluginDescriptorLoader.load(plugin);
    verify(artifactPluginDescriptorFactory).create(plugin, empty());
  }

}
