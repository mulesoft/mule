/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class TemporaryApplicationDescriptorFactoryTestCase extends AbstractMuleTestCase {

  public static final String APP_NAME = "testApp";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;
  private TemporaryApplicationDescriptorFactory temporaryApplicationDescriptorFactory;
  private File rootArtifactFolder;
  private ApplicationDescriptor applicationDescriptor;

  @Before
  public void before() throws IOException {
    this.artifactPluginDescriptorLoader = mock(ArtifactPluginDescriptorLoader.class);
    this.temporaryApplicationDescriptorFactory = new TemporaryApplicationDescriptorFactory(
                                                                                           artifactPluginDescriptorLoader,
                                                                                           new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry()));

    this.rootArtifactFolder = temporaryFolder.newFolder();

    this.applicationDescriptor = mock(ApplicationDescriptor.class);
    when(this.applicationDescriptor.getRootFolder()).thenReturn(this.rootArtifactFolder);
    when(this.applicationDescriptor.getName()).thenReturn(APP_NAME);
  }

  @After
  public void after() {
    verify(applicationDescriptor).getRootFolder();
    verify(applicationDescriptor).getName();
  }

  @Test
  public void relativeAppClassesFolder() throws Exception {
    File folder = this.temporaryApplicationDescriptorFactory.getAppClassesFolder(applicationDescriptor);
    assertThat(folder.getParentFile().getParentFile(), equalTo(rootArtifactFolder));
    File standaloneAppClassFolder = MuleFoldersUtil.getAppClassesFolder(APP_NAME);
    assertRelativeFolder(folder, standaloneAppClassFolder);
  }

  private void assertRelativeFolder(File folder, File standaloneAppClassFolder) {
    assertThat(standaloneAppClassFolder.toPath().endsWith(rootArtifactFolder.toPath().relativize(folder.toPath())), is(true));
  }

  @Test
  public void relativeAppLibFolder() throws Exception {
    File folder = this.temporaryApplicationDescriptorFactory.getAppLibFolder(applicationDescriptor);
    assertThat(folder.getParentFile().getParentFile(), equalTo(rootArtifactFolder));
    File standaloneAppLibFolder = MuleFoldersUtil.getAppLibFolder(APP_NAME);
    assertRelativeFolder(folder, standaloneAppLibFolder);
  }

  @Test
  public void relativeAppSharedPluginLibsFolder() throws Exception {
    File folder = this.temporaryApplicationDescriptorFactory.getAppSharedLibsFolder(applicationDescriptor);
    assertThat(folder.getParentFile().getParentFile().getParentFile(), equalTo(rootArtifactFolder));
    File standaloneAppSharedLibsFolder = MuleFoldersUtil.getAppSharedLibsFolder(APP_NAME);
    assertRelativeFolder(folder, standaloneAppSharedLibsFolder);
  }

}
