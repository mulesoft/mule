/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader;

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
