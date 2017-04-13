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
import org.mule.runtime.module.extension.internal.runtime.resolver.RegistryLookupValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * Default {@link ValueResolverParsingDelegate} which accepts any {@link MetadataType} and parses it by performing a registry
 * lookup
 *
 * @since 4.0
 */
public class DefaultValueResolverParsingDelegate implements ValueResolverParsingDelegate {

  /**
   * @param metadataType a {@link MetadataType}
   * @return {@code true}
   */
  @Override
  public boolean accepts(MetadataType metadataType) {
    return true;
  }

  /**
   * @param key the parsed entity key
   * @param metadataType a {@link MetadataType}
   * @param dslElementResolver
   * @return A {@link ValueResolver} that performs a registry lookup using the given {@code key}
   */
  @Override
  public ValueResolver<Object> parse(String key, MetadataType metadataType, DslElementSyntax elementDsl) {
    return new RegistryLookupValueResolver<>(key);
  }
}
