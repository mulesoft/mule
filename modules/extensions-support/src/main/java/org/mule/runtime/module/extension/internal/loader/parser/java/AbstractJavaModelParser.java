/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.extension.api.loader.parser.AdditionalPropertiesModelParser;

import java.util.LinkedList;
import java.util.List;

/**
 * Base class for Java written extension model parsers
 *
 * @since 4.5.0
 */
abstract class AbstractJavaModelParser implements AdditionalPropertiesModelParser {

  protected final ExtensionElement extensionElement;
  protected final ExtensionLoadingContext loadingContext;
  protected final List<ModelProperty> additionalModelProperties = new LinkedList<>();

  /**
   * Creates a new instance
   *
   * @param extensionElement the extension element
   * @param loadingContext   the loading context
   */
  public AbstractJavaModelParser(ExtensionElement extensionElement, ExtensionLoadingContext loadingContext) {
    this.extensionElement = extensionElement;
    this.loadingContext = loadingContext;
  }

  /**
   * Returns a list with all the {@link ModelProperty model properties} to be applied at the extension level which are
   * specifically linked to the type of syntax used to define the extension.
   *
   * @return a list with {@link ModelProperty} instances.
   */
  @Override
  public final List<ModelProperty> getAdditionalModelProperties() {
    return additionalModelProperties;
  }

}
