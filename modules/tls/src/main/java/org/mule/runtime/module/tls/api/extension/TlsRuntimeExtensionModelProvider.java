/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.api.extension;

import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getTlsExtensionModel;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider;

public final class TlsRuntimeExtensionModelProvider implements RuntimeExtensionModelProvider {

  @Override
  public ExtensionModel createExtensionModel() {
    return getTlsExtensionModel();
  }
}
