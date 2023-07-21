/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.metadata.api.utils.MetadataTypeUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getMetadataResolverFactory;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;

import java.util.Optional;

/**
 * Base implementation for the Metadata service delegate implementations that are used by the {@link MetadataMediator}
 *
 * @since 4.0
 */
abstract class BaseMetadataDelegate {

  final static String NULL_TYPE_ERROR = "NullType is not a valid type for this element";

  protected final EnrichableModel model;
  final MetadataResolverFactory resolverFactory;

  BaseMetadataDelegate(EnrichableModel model) {
    this.model = model;
    this.resolverFactory = getMetadataResolverFactory(model);
  }

  boolean isMetadataResolvedCorrectly(MetadataType dynamicType, boolean allowsNullType) {
    return dynamicType != null && (!isVoid(dynamicType) || allowsNullType);
  }

  Optional<NamedTypeResolver> getOptionalResolver(NamedTypeResolver resolver) {
    return resolver instanceof NullMetadataResolver ? Optional.empty() : Optional.of(resolver);
  }
}
