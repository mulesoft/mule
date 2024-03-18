/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.types;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.metadata.api.dsl.DslElementModel;

import java.util.Optional;

/**
 * Implementation of {@link MetadataResolutionTypeInformation} that describes Attribute Types from a {@link ComponentAst}
 *
 * @since 4.2.0
 */
public class AttributesMetadataResolutionTypeInformation extends AbstractMetadataResolutionTypeInformation {

  private static final String TYPE_IDENTIFIER = "Attributes";
  private static final MetadataCacheId COMPONENT_TYPE_METADATA_CACHE_ID =
      new MetadataCacheId(TYPE_IDENTIFIER.hashCode(), TYPE_IDENTIFIER);

  public AttributesMetadataResolutionTypeInformation(ComponentAst component) {
    super(component, (typeResolversInformationModelProperty -> getResolverInformation(typeResolversInformationModelProperty)));
    checkArgument(component.getModel(HasOutputModel.class).isPresent(),
                  "Cannot generate an Attribute Cache Key for a component with no output");
  }

  public AttributesMetadataResolutionTypeInformation(DslElementModel<?> component) {
    super(component, (typeResolversInformationModelProperty -> getResolverInformation(typeResolversInformationModelProperty)));
    checkArgument(component.getModel() != null, "Cannot generate an Attribute Cache Key for a 'null' component");
    checkArgument(component.getModel() instanceof HasOutputModel,
                  "Cannot generate an Attribute Cache Key for a component with no output");
  }

  public AttributesMetadataResolutionTypeInformation(ComponentParameterization<?> component) {
    super(component, (typeResolversInformationModelProperty -> getResolverInformation(typeResolversInformationModelProperty)));
    checkArgument(component.getModel() != null, "Cannot generate an Attribute Cache Key for a 'null' component");
    checkArgument(component.getModel() instanceof HasOutputModel,
                  "Cannot generate an Attribute Cache Key for a component with no output");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataCacheId getComponentTypeMetadataCacheId() {
    return COMPONENT_TYPE_METADATA_CACHE_ID;
  }

  private static Optional<ResolverInformation> getResolverInformation(TypeResolversInformationModelProperty typeResolversInformationModelProperty) {
    return typeResolversInformationModelProperty.getAttributesResolver();
  }
}
