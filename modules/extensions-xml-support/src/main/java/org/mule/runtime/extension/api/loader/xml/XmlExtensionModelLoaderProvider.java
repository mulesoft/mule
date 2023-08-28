/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.extension.api.loader.xml;

import static java.util.Collections.singleton;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider;
import org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader;

import java.util.Set;

public class XmlExtensionModelLoaderProvider implements ExtensionModelLoaderProvider {

  @Override
  public Set<ExtensionModelLoader> getExtensionModelLoaders() {
    return singleton(new XmlExtensionModelLoader());
  }
}
