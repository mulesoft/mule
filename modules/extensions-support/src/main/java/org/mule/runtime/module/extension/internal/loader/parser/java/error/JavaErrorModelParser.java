/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.error;

import static org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParserUtils.getDeclarationClass;

import static java.util.Objects.hash;
import static java.util.Optional.empty;

import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * {@link ErrorModelParser} implementation for errors defined through the Java language API.
 *
 * @since 4.5.0
 */
public class JavaErrorModelParser implements ErrorModelParser {

  private final String extensionNamespace;
  private final ErrorTypeDefinition<?> errorTypeDefinition;
  private final Class<?> errorTypeDefinitionDeclarationClass;
  private Optional<ErrorModelParser> parent = empty();

  /**
   * Create a new instance
   *
   * @param errorTypeDefinition the {@link ErrorTypeDefinition}
   * @param extensionNamespace  the namespace of the extension that is being parsed.
   */
  public JavaErrorModelParser(ErrorTypeDefinition<?> errorTypeDefinition, String extensionNamespace) {
    this.extensionNamespace = extensionNamespace;
    this.errorTypeDefinition = errorTypeDefinition;
    errorTypeDefinitionDeclarationClass = getDeclarationClass(errorTypeDefinition);
  }

  @Override
  public String getType() {
    return errorTypeDefinition.getType();
  }

  @Override
  public String getNamespace() {
    return extensionNamespace;
  }

  public void setParent(Optional<ErrorModelParser> parent) {
    this.parent = parent;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JavaErrorModelParser that = (JavaErrorModelParser) o;
    return Objects.equals(errorTypeDefinitionDeclarationClass, that.errorTypeDefinitionDeclarationClass)
        && Objects.equals(this.extensionNamespace, that.extensionNamespace)
        && Objects.equals(this.errorTypeDefinition, that.errorTypeDefinition);
  }

  @Override
  public int hashCode() {
    return hash(errorTypeDefinitionDeclarationClass, extensionNamespace, errorTypeDefinition);
  }
}
