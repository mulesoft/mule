/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.extension;


import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_MULE_SDK_PROPERTY;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getOperationDslExtensionModel;

import static java.lang.Boolean.getBoolean;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider;

/**
 * Provides the {@link ExtensionModel} for operations Mule DSL
 *
 * @since 4.5.0
 */
public final class OperationDslExtensionModelProvider implements RuntimeExtensionModelProvider {

  private final boolean isMuleSdkEnabled = getBoolean(ENABLE_MULE_SDK_PROPERTY);

  @Override
  public ExtensionModel createExtensionModel() {
    return isMuleSdkEnabled ? getOperationDslExtensionModel() : null;
  }
}
