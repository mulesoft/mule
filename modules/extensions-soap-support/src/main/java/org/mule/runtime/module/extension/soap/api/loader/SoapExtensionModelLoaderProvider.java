/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.api.loader;

import static java.util.Collections.singleton;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider;
import org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader;

import java.util.Set;

/**
 * {@link ExtensionModelLoaderProvider} yielding loaders for SOAP based extensions
 *
 * @since 4.5.0
 */
public class SoapExtensionModelLoaderProvider implements ExtensionModelLoaderProvider {

  @Override
  public Set<ExtensionModelLoader> getExtensionModelLoaders() {
    return singleton(new SoapExtensionModelLoader());
  }
}
