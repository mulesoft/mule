/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import static org.mule.test.allure.AllureConstants.ExtensionModelDiscoveryFeature.EXTENSION_MODEL_DISCOVERY;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;

import io.qameta.allure.Feature;

@Feature(EXTENSION_MODEL_DISCOVERY)
public class TestRuntimeExtensionModelProvider implements RuntimeExtensionModelProvider {

  @Override
  public ExtensionModel createExtensionModel() {
    ExtensionModel extModel = mock(ExtensionModel.class);
    when(extModel.getName()).thenReturn("testRuntime");
    return extModel;
  }

}
