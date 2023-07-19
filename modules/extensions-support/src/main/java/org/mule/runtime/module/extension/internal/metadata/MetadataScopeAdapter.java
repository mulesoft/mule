/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Adapter implementation which expands the {@link MetadataScope} to a more descriptive of the developer's metadata declaration
 * for a {@link ComponentModel component}
 *
 * @since 4.0
 */
public interface MetadataScopeAdapter {

  default boolean isCustomScope() {
    return hasOutputResolver() || hasInputResolvers();
  }

  boolean isPartialKeyResolver();

  boolean hasKeysResolver();

  boolean hasInputResolvers();

  boolean hasOutputResolver();

  boolean hasAttributesResolver();

  TypeKeysResolver getKeysResolver();

  Map<String, Supplier<? extends InputTypeResolver>> getInputResolvers();

  OutputTypeResolver getOutputResolver();

  AttributesTypeResolver getAttributesResolver();

  MetadataType getKeyResolverMetadataType();

  String getKeyResolverParameterName();
}
