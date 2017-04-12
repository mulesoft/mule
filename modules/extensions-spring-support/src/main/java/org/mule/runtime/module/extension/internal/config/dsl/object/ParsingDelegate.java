/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

/**
 * A simple delegate interface for optionally parsing entities which type is represented by {@code M} and produce a value of type
 * {@code T}.
 * <p>
 * To know if this delegate is suitable to parse a given entity, call the {@link #accepts(MetadataType)} method. If it returns
 * true, then you can use the {@link #parse(String, MetadataType, DslSyntaxResolver)} to obtain a value (do not call this method
 * if the instance didn't accept the type).
 *
 * @param <M> the generic type of the accepted {@link MetadataType}s
 * @param <T> the generic type of the produced values
 * @since 4.0
 */
public interface ParsingDelegate<M extends MetadataType, T> {

  /**
   * Verifies that {@code this} instance is capable of handling a specific type.
   *
   * @param metadataType a {@link MetadataType}
   * @return whether it's safe to call {@link #parse(String, MetadataType, DslSyntaxResolver)} or not
   */
  boolean accepts(M metadataType);

  /**
   * Performs the parsing and returns a value.
   * <p>
   * This method should only be invoked if {@link #accepts(MetadataType)} returns {@code true} for the same {@code metadataType}
   *
   * @param key the parsed entity key
   * @param metadataType a {@link MetadataType}
   * @param elementDsl the {@link DslElementSyntax} of the parsed element
   * @return
   */
  T parse(String key, M metadataType, DslElementSyntax elementDsl);
}
