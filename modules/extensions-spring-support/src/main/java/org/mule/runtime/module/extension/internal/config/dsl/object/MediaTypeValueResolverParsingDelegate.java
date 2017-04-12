/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * A {@link ValueResolverParsingDelegate} for parsing instances of {@link MediaType}
 *
 * @since 4.0
 */
public class MediaTypeValueResolverParsingDelegate implements ValueResolverParsingDelegate {

  @Override
  public boolean accepts(MetadataType metadataType) {
    return MediaType.class.equals(getType(metadataType));
  }

  @Override
  public ValueResolver<Object> parse(String key, MetadataType metadataType, DslElementSyntax elementDsl) {
    return new StaticValueResolver<>(DataType.builder().mediaType(key).build().getMediaType());
  }
}
