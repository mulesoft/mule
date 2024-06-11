/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;

/**
 * Utilities for handling an extension's namespace
 * <p>
 * This class <b>IS NOT</b> part of the API. To be used by the Mule Runtime only
 *
 * @since 4.8.0
 */
public class ExtensionNamespaceUtils {

  /**
   * @param extensionModel an {@link ExtensionModel}
   * @return the extension's namespace for a given {@link ExtensionModel}
   */
  public static String getExtensionsNamespace(ExtensionModel extensionModel) {
    return getExtensionsNamespace(extensionModel.getXmlDslModel());
  }

  /**
   * @param extensionDeclaration a {@link ExtensionDeclaration}
   * @return the extension's namespace for a given {@link ExtensionDeclaration}
   */
  public static String getExtensionsNamespace(ExtensionDeclaration extensionDeclaration) {
    return getExtensionsNamespace(extensionDeclaration.getXmlDslModel());
  }

  /**
   * @param dslModel a {@link XmlDslModel}
   * @return the extension's namespace for a given {@link ExtensionDeclaration}
   */
  public static String getExtensionsNamespace(XmlDslModel dslModel) {
    return dslModel.getPrefix().toUpperCase();
  }
}
