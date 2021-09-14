/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.error;

import org.mule.runtime.module.extension.internal.error.LegacyErrorTypeDefinitionAdapter;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Optional;

public class JavaErrorModelParser implements ErrorModelParser {

  private final Optional<ErrorModelParser> parent;
  private final ErrorTypeDefinition<?> errorTypeDefinition;
  private final Class<?> errorTypeDefinitionDeclarationClass;

  public JavaErrorModelParser(ErrorTypeDefinition<?> errorTypeDefinition, Optional<ErrorModelParser> parent) {
    this.errorTypeDefinition = errorTypeDefinition;
    this.parent = parent;
    errorTypeDefinitionDeclarationClass = (errorTypeDefinition instanceof LegacyErrorTypeDefinitionAdapter)
        ? ((LegacyErrorTypeDefinitionAdapter<?>) errorTypeDefinition).getDelegate().getClass()
        : errorTypeDefinition.getClass();
  }

  @Override
  public String getType() {
    return errorTypeDefinition.getType();
  }

  @Override
  public Optional<ErrorModelParser> getParent() {
    return parent;
  }

  public ErrorTypeDefinition<?> getErrorTypeDefinition() {
    return errorTypeDefinition;
  }

  public Class<?> getErrorTypeDefinitionDeclarationClass() {
    return errorTypeDefinitionDeclarationClass;
  }
}
