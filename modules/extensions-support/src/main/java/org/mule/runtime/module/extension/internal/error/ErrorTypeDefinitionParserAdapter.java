/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;

import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Adapts an {@link ErrorModelParser} into an {@link ErrorTypeDefinition}
 *
 * @param <E> the definition's generic type
 * @since 4.5.0
 */
public class ErrorTypeDefinitionParserAdapter<E extends Enum<E>> implements ErrorTypeDefinition<E> {

  private final ErrorModelParser parser;

  public ErrorTypeDefinitionParserAdapter(ErrorModelParser parser) {
    this.parser = parser;
  }

  @Override
  public String getType() {
    return parser.getType();
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return parser.getParent().map(ErrorTypeDefinitionParserAdapter::new);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorTypeDefinitionParserAdapter<?> that = (ErrorTypeDefinitionParserAdapter<?>) o;
    return Objects.equals(parser, that.parser);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parser);
  }
}
