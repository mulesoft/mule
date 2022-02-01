/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.internal.manager.CompositeArtifactExtensionManager;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class CompositeArtifactExtensionManagerFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void createsCompositeExtensionManager() throws Exception {

    Application application = mock(Application.class, RETURNS_DEEP_STUBS);

    final Registry registry = mock(Registry.class);
    when(application.getRegistry()).thenReturn(registry);
    final ArtifactContext artifactContext = mock(ArtifactContext.class);
    when(artifactContext.getRegistry()).thenReturn(registry);
    when(application.getArtifactContext()).thenReturn(artifactContext);

    ExtensionManager applicationExtensionManager = mock(ExtensionManager.class);
    when(registry.lookupByName(OBJECT_EXTENSION_MANAGER))
        .thenReturn(of(applicationExtensionManager));

    ExtensionModelLoaderRepository extensionModelLoaderRepository = mock(ExtensionModelLoaderRepository.class);

    ExtensionManagerFactory extensionManagerFactory = mock(ExtensionManagerFactory.class);
    CompositeArtifactExtensionManagerFactory factory = new CompositeArtifactExtensionManagerFactory(application,
                                                                                                    extensionModelLoaderRepository,
                                                                                                    emptyList(),
                                                                                                    extensionManagerFactory);

    ExtensionManager policyExtensionManager = mock(ExtensionManager.class);
    MuleContext muleContext = mock(MuleContext.class);
    when(extensionManagerFactory.create(muleContext)).thenReturn(policyExtensionManager);

    ExtensionManager extensionManager = factory.create(muleContext);

    assertThat(extensionManager, instanceOf(CompositeArtifactExtensionManager.class));
    CompositeArtifactExtensionManager compositeArtifactExtensionManager = (CompositeArtifactExtensionManager) extensionManager;
    assertThat(compositeArtifactExtensionManager.getParentExtensionManager(), equalTo(applicationExtensionManager));
    assertThat(compositeArtifactExtensionManager.getChildExtensionManager(), equalTo(policyExtensionManager));
  }
}
