/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclarer;

/**
 * Parses the syntactic definition of a {@link OutputModel} so that the semantics reflected in it can be extracted in a uniform
 * way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface OutputModelParser {

  /**
   * @return the {@link MetadataType} for the output value
   */
  MetadataType getType();

  /**
   * @return whether the type is dynamic
   */
  boolean isDynamic();

  /**
   * Configures the given {@code declarer} with the values extracted by {@code this} parser
   *
   * @param declarer an {@link OutputDeclarer}
   * @param <T>      the declarer's generic type
   * @return the configured declarer
   */
  default <T extends OutputDeclarer> OutputDeclarer<T> applyOn(OutputDeclarer<T> declarer) {
    if (isDynamic()) {
      declarer.ofDynamicType(getType());
    } else {
      declarer.ofType(getType());
    }

    return declarer;
  }
}
