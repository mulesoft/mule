/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.types;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;

import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract implementation of {@link MetadataResolutionTypeInformation} that is based on a {@link DslElementModel}
 *
 * @since 4.2.0
 */
public abstract class AbstractMetadataResolutionTypeInformation implements MetadataResolutionTypeInformation {

  private String resolverCategory;
  private String resolverName;

  public AbstractMetadataResolutionTypeInformation(DslElementModel<?> component,
                                                   Function<TypeResolversInformationModelProperty, Optional<ResolverInformation>> getResolverInformationFromModelProperty) {
    if (component.getModel() instanceof EnrichableModel) {
      Optional<TypeResolversInformationModelProperty> typeResolversInformationModelProperty =
          ((EnrichableModel) component.getModel()).getModelProperty(TypeResolversInformationModelProperty.class);
      if (typeResolversInformationModelProperty.isPresent()) {
        resolverName = getResolverInformationFromModelProperty.apply(typeResolversInformationModelProperty.get())
            .map(resolverInformation -> resolverInformation.getResolverName()).orElse(null);
        resolverCategory = typeResolversInformationModelProperty.get().getCategoryName();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamicType() {
    return resolverCategory != null && resolverName != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> getResolverName() {
    return ofNullable(resolverName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> getResolverCategory() {
    return ofNullable(resolverCategory);
  }
}
