/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.types;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;

import java.util.Optional;

/**
 * Implementation of {@link MetadataResolutionTypeInformation} that describes Input Types from a {@link DslElementModel}
 *
 * @since 4.2.0
 */
public class InputMetadataResolutionTypeInformation extends AbstractMetadataResolutionTypeInformation {

  public static final String TYPE_IDENTIFIER = "Input";
  private final MetadataCacheId componentTypeMetadataCacheId;

  public InputMetadataResolutionTypeInformation(DslElementModel<?> component, String parameterName) {
    super(component, (typeResolversInformationModelProperty -> getResolverInformation(typeResolversInformationModelProperty,
                                                                                      parameterName)));
    checkArgument(component.getModel() != null, "Cannot generate an Input Cache Key for a 'null' component");
    checkArgument(component.getModel() instanceof ParameterizedModel,
                  "Cannot generate an Input Cache Key for a component with no parameters");
    checkArgument(((ParameterizedModel) component.getModel()).getAllParameterModels().stream()
        .anyMatch(parameterModel -> parameterModel.getName().equals(parameterName)),
                  "Cannot generate an Input Cache Key for the component since it does not have a parameter named "
                      + parameterName);

    String parameterTypeIdentifier;
    if (isDynamicType()) {
      parameterTypeIdentifier = TYPE_IDENTIFIER;
    } else {
      parameterTypeIdentifier = String.format("%s with parameter name : %s", TYPE_IDENTIFIER, parameterName);
    }
    componentTypeMetadataCacheId = new MetadataCacheId(parameterTypeIdentifier.hashCode(), parameterTypeIdentifier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataCacheId getComponentTypeMetadataCacheId() {
    return componentTypeMetadataCacheId;
  }

  protected static Optional<ResolverInformation> getResolverInformation(TypeResolversInformationModelProperty typeResolversInformationModelProperty,
                                                                        String parameterName) {
    return typeResolversInformationModelProperty.getParameterResolver(parameterName);
  }
}
