/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;

import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Optional;

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
}
