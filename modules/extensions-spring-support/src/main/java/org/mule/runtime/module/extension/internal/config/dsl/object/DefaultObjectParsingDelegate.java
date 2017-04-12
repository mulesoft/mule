/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * Default {@link ObjectParsingDelegate} which accepts any {@link ObjectType} and parses it as a {@link ValueResolver}
 *
 * @since 4.0
 */
public class DefaultObjectParsingDelegate implements ObjectParsingDelegate {

  /**
   * @param objectType an {@link ObjectType}
   * @return {@code true}
   */
  @Override
  public boolean accepts(ObjectType objectType) {
    return true;
  }

  /**
   * Parses the given {@code objectType} as a {@link ValueResolver}
   *
   * @param name the element name
   * @param objectType a {@link ObjectType}
   * @param elementDsl the {@link DslElementSyntax} of the parsed element
   * @return a {@link AttributeDefinition.Builder}
   */
  @Override
  public AttributeDefinition.Builder parse(String name, ObjectType objectType, DslElementSyntax elementDsl) {
    AttributeDefinition.Builder builder = fromChildConfiguration(ValueResolver.class);
    if (elementDsl.isWrapped()) {
      builder.withWrapperIdentifier(elementDsl.getElementName());
    } else {
      builder.withIdentifier(elementDsl.getElementName());
    }

    return builder;
  }
}
