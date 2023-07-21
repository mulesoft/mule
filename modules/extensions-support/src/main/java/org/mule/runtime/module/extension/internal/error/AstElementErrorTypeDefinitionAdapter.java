/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.error;

import org.mule.sdk.api.error.ErrorTypeDefinition;

import javax.lang.model.element.Element;

/**
 * Adapts a Java AST {@link Element} into an sdk-api {@link ErrorTypeDefinition}.
 *
 * This is useful when parsing the extension not from a compliled class but from a Java AST.
 *
 * @param <E> the definition's generic type
 * @since 4.5.0
 */
public class AstElementErrorTypeDefinitionAdapter<E extends Enum<E>> implements ErrorTypeDefinition<E> {

  private final String type;

  public AstElementErrorTypeDefinitionAdapter(Element element) {
    type = element.toString();
  }

  @Override
  public String getType() {
    return type;
  }
}
