/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.module.extension.internal.loader.java.property.DevelopmentFrameworkModelProperty;

/**
 * The main extension development frameworks.
 *
 * @since 4.5, moved form extensions-api in 4.8
 */
public enum ExtensionDevelopmentFramework {

  JAVA_SDK, XML_SDK, MULE_SDK, MULE_DSL;

  /**
   * @param extensionModel The {@link ExtensionModel}.
   * @return The {@link ExtensionDevelopmentFramework} used to develop the extension represented by given {@code extensionModel}.
   *         Defaults to {@link ExtensionDevelopmentFramework#JAVA_SDK} if it cannot be determined.
   */
  public static ExtensionDevelopmentFramework getExtensionDevelopmentFramework(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(DevelopmentFrameworkModelProperty.class)
        .map(DevelopmentFrameworkModelProperty::getDevelopmentFramework)
        .orElse(JAVA_SDK);
  }

  /**
   * @param extensionDeclaration The {@link ExtensionDeclaration}.
   * @return The {@link ExtensionDevelopmentFramework} used to develop the extension represented by the given
   *         {@code extensionDeclaration}. Defaults to {@link ExtensionDevelopmentFramework#JAVA_SDK} if it cannot be determined.
   */
  public static ExtensionDevelopmentFramework getExtensionDevelopmentFramework(ExtensionDeclaration extensionDeclaration) {
    return extensionDeclaration.getModelProperty(DevelopmentFrameworkModelProperty.class)
        .map(DevelopmentFrameworkModelProperty::getDevelopmentFramework)
        .orElse(JAVA_SDK);
  }

  /**
   *
   * @param extensionModel The {@link ExtensionModel}.
   * @param expected       The {@link ExtensionDevelopmentFramework} we want to check if the extension was developed with.
   * @return Whether the given {@code extensionModel} corresponds to an extension developed with the {@code expected}
   *         {@link ExtensionDevelopmentFramework}.
   */
  public static boolean isExtensionDevelopmentFramework(ExtensionModel extensionModel, ExtensionDevelopmentFramework expected) {
    return getExtensionDevelopmentFramework(extensionModel).equals(expected);
  }

  /**
   *
   * @param extensionDeclaration The {@link ExtensionDeclaration}.
   * @param expected             The {@link ExtensionDevelopmentFramework} we want to check if the extension was developed with.
   * @return Whether the given {@code extensionDeclaration} corresponds to an extension developed with the {@code expected}
   *         {@link ExtensionDevelopmentFramework}.
   */
  public static boolean isExtensionDevelopmentFramework(ExtensionDeclaration extensionDeclaration,
                                                        ExtensionDevelopmentFramework expected) {
    return getExtensionDevelopmentFramework(extensionDeclaration).equals(expected);
  }
}
