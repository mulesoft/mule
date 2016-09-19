/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ApplicationExtensionsManagerConfigurationBuilderTestCase extends AbstractMuleTestCase {

  private static final String MANIFEST_RESOURCE = "META-INF/" + EXTENSION_MANIFEST_FILE_NAME;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ArtifactPlugin extensionPlugin;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ArtifactPlugin notExtensionPlugin;

  @Mock
  private ExtensionManagerAdapterFactory extensionManagerAdapterFactory;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionManagerAdapter extensionManager;

  @Mock
  private ClassLoader pluginClassLoader;

  @Mock
  private ExtensionManifest manifest;

  private ApplicationExtensionsManagerConfigurationBuilder builder;

  @Before
  public void before() throws Exception {
    when(extensionPlugin.getArtifactClassLoader().findResource(MANIFEST_RESOURCE)).thenReturn(new URL("file:/blah"));
    when(extensionPlugin.getArtifactClassLoader().getClassLoader()).thenReturn(pluginClassLoader);
    when(notExtensionPlugin.getArtifactClassLoader().findResource(MANIFEST_RESOURCE)).thenReturn(null);

    when(extensionManagerAdapterFactory.createExtensionManager(muleContext)).thenReturn(extensionManager);
    when(extensionManager.parseExtensionManifestXml(any())).thenReturn(manifest);

    builder = new ApplicationExtensionsManagerConfigurationBuilder(asList(extensionPlugin, notExtensionPlugin),
                                                                   extensionManagerAdapterFactory);
  }

  @Test
  public void register() throws Exception {
    builder.doConfigure(muleContext);
    ArgumentCaptor<ExtensionManifest> manifestCaptor = forClass(ExtensionManifest.class);

    verify(extensionManager).registerExtension(manifestCaptor.capture(), same(pluginClassLoader));
    ExtensionManifest manifest = manifestCaptor.getValue();
    assertThat(manifest, is(sameInstance(manifest)));
  }
}
