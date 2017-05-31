/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.utils;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.CraftedExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader;

public class ExtensionLoaderUtils {

  public static ExtensionModelLoader getLoaderById(String id) {
    switch (id) {
      case SoapExtensionModelLoader.SOAP_LOADER_ID:
        return new SoapExtensionModelLoader();
      case CraftedExtensionModelLoader.CRAFTED_LOADER_ID:
        return new CraftedExtensionModelLoader();
      case DefaultJavaExtensionModelLoader.JAVA_LOADER_ID:
        return new DefaultJavaExtensionModelLoader();
      default:
        throw new RuntimeException("No loader found for id:{" + id + "}");
    }
  }
}
