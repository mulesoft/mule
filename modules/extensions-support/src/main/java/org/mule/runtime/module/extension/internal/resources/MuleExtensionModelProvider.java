/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule's Runtime
 *
 * @since 4.0
 */
public class MuleExtensionModelProvider {

  private static final String MODEL_JSON = "META-INF/mule-extension-model.json";
  private static final ExtensionModel EXTENSION_MODEL = new ExtensionModelJsonSerializer(false)
      .deserialize(IOUtils.toString(MuleExtensionModelProvider.class.getClassLoader()
          .getResourceAsStream(MODEL_JSON)));

  /**
   * @return the {@link ExtensionModel} definition for Mule's Runtime
   */
  public static ExtensionModel getMuleExtensionModel() {
    return EXTENSION_MODEL;
  }

}
