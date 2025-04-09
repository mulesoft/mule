/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceAnnotation;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;

/**
 * A generic contract for any kind of component that can contain an alias name or description
 *
 * @since 4.0
 */
@NoImplement
public interface WithAlias extends WithAnnotations, WithName {

  String EMPTY = "";

  /**
   * @return The alias of the implementer component
   */
  default String getAlias() {
    return mapReduceAnnotation(this,
                               Alias.class,
                               org.mule.sdk.api.annotation.Alias.class,
                               value -> value.getStringValue(Alias::value),
                               value -> value.getStringValue(org.mule.sdk.api.annotation.Alias::value),
                               () -> new IllegalModelDefinitionException(format("Both %s and %s annotations are present on element '%s'",
                                                                                Alias.class.getName(),
                                                                                org.mule.sdk.api.annotation.Alias.class.getName(),
                                                                                getName())))
        .orElseGet(this::getName);
  }

  /**
   * @return The description of the implementer component
   */
  default String getDescription() {
    return mapReduceAnnotation(
                               this,
                               Alias.class,
                               org.mule.sdk.api.annotation.Alias.class,
                               value -> value.getStringValue(Alias::description),
                               value -> value.getStringValue(org.mule.sdk.api.annotation.Alias::description),
                               () -> new IllegalModelDefinitionException(format("Both %s and %s annotations are present on element '%s",
                                                                                Alias.class.getName(),
                                                                                org.mule.sdk.api.annotation.Alias.class.getName(),
                                                                                getName())))
        .orElse(EMPTY);
  }
}
