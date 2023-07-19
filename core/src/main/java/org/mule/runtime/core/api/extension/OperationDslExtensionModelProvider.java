/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.extension;


import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_MULE_SDK_PROPERTY;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getOperationDslExtensionModel;

import static java.lang.Boolean.getBoolean;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider;

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
