/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.extension;


import static org.mule.runtime.extension.api.extension.XmlSdk1ExtensionModelProvider.getExtensionModel;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider;

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
