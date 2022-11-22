/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.extension;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_MULE_SDK_PROPERTY;
import static org.mule.runtime.module.extension.mule.api.extension.MuleSdkExtensionExtensionModelProvider.getExtensionModel;

import static java.lang.Boolean.getBoolean;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;

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
