/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.extension.api.extension;


import static org.mule.runtime.extension.api.extension.XmlSdk1ExtensionModelProvider.getExtensionModel;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider;

/**
 * A {@link RuntimeExtensionModelProvider} for Mule's XML SDK v1.
 *
 * @since 4.4
 */
@NoInstantiate
public class XmlSdk1RuntimeExtensionModelProvider implements RuntimeExtensionModelProvider {

  @Override
  public ExtensionModel createExtensionModel() {
    return getExtensionModel();
  }

}
