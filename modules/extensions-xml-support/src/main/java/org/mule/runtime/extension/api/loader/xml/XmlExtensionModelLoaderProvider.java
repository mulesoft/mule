/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.loader.xml;

import static java.util.Collections.singleton;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.loader.ExtensionModelLoaderProvider;

import java.util.Set;

public class XmlExtensionModelLoaderProvider
    implements ExtensionModelLoaderProvider, org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider {

  @Override
  public Set<ExtensionModelLoader> getExtensionModelLoaders() {
    return singleton(new XmlExtensionModelLoader());
  }
}
