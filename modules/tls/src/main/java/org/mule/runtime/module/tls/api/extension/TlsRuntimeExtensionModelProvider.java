/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
