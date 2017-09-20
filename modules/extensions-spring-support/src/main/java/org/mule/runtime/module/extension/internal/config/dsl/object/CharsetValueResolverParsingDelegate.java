/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import static org.mule.runtime.api.metadata.MediaTypeUtils.parseCharset;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.nio.charset.Charset;

/**
 * A {@link ValueResolverParsingDelegate} for parsing instances of {@link Charset}
 *
 * @since 4.0
 */
public class CharsetValueResolverParsingDelegate implements ValueResolverParsingDelegate {

  /**
   * @param metadataType a {@link MetadataType}
   * @return {@code true} if {@code metadataType} represents a {@link Charset}
   */
  @Override
  public boolean accepts(MetadataType metadataType) {
    return getType(metadataType).map(Charset.class::equals).orElse(false);
  }

  /**
   * @param key the parsed entity key
   * @param metadataType a {@link MetadataType}
   * @param elementDsl the {@link DslElementSyntax} of the parsed element
   * @return A {@link ValueResolver} which resolves to the {@link Charset} corresponding with the given {@code key}
   */
  @Override
  public ValueResolver<Object> parse(String key, MetadataType metadataType, DslElementSyntax elementDsl) {
    return new StaticValueResolver<>(parseCharset(key));
  }
}
