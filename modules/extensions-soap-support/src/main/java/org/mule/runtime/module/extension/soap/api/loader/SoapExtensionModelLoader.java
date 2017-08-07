/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.api.loader;

import org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader;
import org.mule.runtime.module.extension.soap.internal.loader.SoapModelLoaderDelegate;

/**
 * Loads a Soap Based extension by introspecting a class which uses the Soap Extensions API annotations
 *
 * @since 4.0
 */
public class SoapExtensionModelLoader extends AbstractJavaExtensionModelLoader {

  public static final String SOAP_LOADER_ID = "soap";

  public SoapExtensionModelLoader() {
    super(SOAP_LOADER_ID, SoapModelLoaderDelegate::new);
  }

}
