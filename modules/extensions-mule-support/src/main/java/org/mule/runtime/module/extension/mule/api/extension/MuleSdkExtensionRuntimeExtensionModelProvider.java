/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.mule.api.extension;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_MULE_SDK_PROPERTY;
import static org.mule.runtime.module.extension.mule.api.extension.MuleSdkExtensionExtensionModelProvider.getExtensionModel;

import static java.lang.Boolean.getBoolean;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider;

/**
 * A {@link RuntimeExtensionModelProvider} for Mule SDK Extensions
 *
 * @since 4.5
 */
@Experimental
@NoInstantiate
public class MuleSdkExtensionRuntimeExtensionModelProvider implements RuntimeExtensionModelProvider {

  private final boolean isMuleSdkEnabled = getBoolean(ENABLE_MULE_SDK_PROPERTY);

  @Override
  public ExtensionModel createExtensionModel() {
    return isMuleSdkEnabled ? getExtensionModel() : null;
  }
}
