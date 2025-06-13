/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.error;

import static org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParserUtils.getDeclarationClass;

import static java.util.Objects.hash;

import org.mule.runtime.module.extension.internal.loader.parser.BaseErrorModelParser;
import org.mule.runtime.extension.api.loader.parser.ErrorModelParser;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * {@link ErrorModelParser} implementation for errors defined through the Java language API.
 *
 * @since 4.5.0
 */
public class JavaErrorModelParser extends BaseErrorModelParser {

  private final ErrorTypeDefinition<?> errorTypeDefinition;
  private final Class<?> errorTypeDefinitionDeclarationClass;

  /**
   * Create a new instance
   *
   * @param errorTypeDefinition the {@link ErrorTypeDefinition}
   * @param namespace           the error namespace of the extension that is being parsed.
   */
  public JavaErrorModelParser(ErrorTypeDefinition<?> errorTypeDefinition, String namespace) {
    super(namespace, errorTypeDefinition.getType());
    this.errorTypeDefinition = errorTypeDefinition;
    this.errorTypeDefinitionDeclarationClass = getDeclarationClass(errorTypeDefinition);
  }

  public void setParent(Optional<ErrorModelParser> parent) {
    super.setParent(parent.orElse(null));
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
        && Objects.equals(this.getNamespace(), that.getNamespace())
        && Objects.equals(this.errorTypeDefinition, that.errorTypeDefinition);
  }

  @Override
  public int hashCode() {
    return hash(errorTypeDefinitionDeclarationClass, getNamespace(), errorTypeDefinition);
  }
}
