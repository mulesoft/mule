/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;

import java.util.LinkedList;
import java.util.List;

abstract class AbstractModelParser {

  protected final ExtensionElement extensionElement;
  protected final ExtensionLoadingContext loadingContext;
  protected final List<ModelProperty> additionalModelProperties = new LinkedList<>();

  public AbstractModelParser(ExtensionElement extensionElement, ExtensionLoadingContext loadingContext) {
    this.extensionElement = extensionElement;
    this.loadingContext = loadingContext;
  }

  public final List<ModelProperty> getAdditionalModelProperties() {
    return additionalModelProperties;
  }
}
