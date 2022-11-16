/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.util.MuleSystemProperties.PARALLEL_EXTENSION_MODEL_LOADING_PROPERTY;
import static org.mule.test.allure.AllureConstants.ExtensionModelDiscoveryFeature.EXTENSION_MODEL_DISCOVERY;
import static org.mule.test.allure.AllureConstants.ExtensionModelDiscoveryFeature.ExtensionModelDiscoveryStory.PARALLEL_EXTENSION_MODEL_LOADING;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.policy.ArtifactExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
@Feature(EXTENSION_MODEL_DISCOVERY)
@Story(PARALLEL_EXTENSION_MODEL_LOADING)
public class ArtifactExtensionManagerFactoryTestCase extends AbstractMuleTestCase {

  private ArtifactExtensionManagerFactory factory;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModelLoaderRepository extensionModelLoaderRepository;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionManagerFactory extensionManagerFactory;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModelDiscoverer discoverer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  @Before
  public void before() {
    factory = new ArtifactExtensionManagerFactory(emptyList(),
                                                  extensionModelLoaderRepository,
                                                  extensionManagerFactory,
                                                  of((v, s) -> discoverer));
  }

  @Test
  public void parallelExtensionModelLoadingDisabledByDefault() {
    ExtensionDiscoveryRequest request = captureExtensionDiscoveryRequest();
    assertThat(request.isParallelDiscovery(), is(false));
  }

  @Test
  public void enableParallelExtensionModelLoading() {
    setProperty(PARALLEL_EXTENSION_MODEL_LOADING_PROPERTY, "true");
    try {
      ExtensionDiscoveryRequest request = captureExtensionDiscoveryRequest();
      assertThat(request.isParallelDiscovery(), is(true));
    } finally {
      clearProperty(PARALLEL_EXTENSION_MODEL_LOADING_PROPERTY);
    }
  }

  @Test
  public void parallelExtensionModelLoadingExplicitlyDisabled() {
    setProperty(PARALLEL_EXTENSION_MODEL_LOADING_PROPERTY, "false");
    try {
      ExtensionDiscoveryRequest request = captureExtensionDiscoveryRequest();
      assertThat(request.isParallelDiscovery(), is(false));
    } finally {
      clearProperty(PARALLEL_EXTENSION_MODEL_LOADING_PROPERTY);
    }
  }

  private ExtensionDiscoveryRequest captureExtensionDiscoveryRequest() {
    factory.create(muleContext);
    ArgumentCaptor<ExtensionDiscoveryRequest> captor = forClass(ExtensionDiscoveryRequest.class);
    verify(discoverer).discoverPluginsExtensionModels(captor.capture());
    ExtensionDiscoveryRequest request = captor.getValue();
    return request;
  }
}
