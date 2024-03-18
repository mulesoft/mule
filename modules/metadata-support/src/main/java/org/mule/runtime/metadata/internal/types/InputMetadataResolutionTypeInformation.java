/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.types;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.metadata.api.dsl.DslElementModel;

import java.util.Optional;

/**
 * Implementation of {@link MetadataResolutionTypeInformation} that describes Input Types from a {@link ComponentAst}
 *
 * @since 4.2.0
 */
public class InputMetadataResolutionTypeInformation extends AbstractMetadataResolutionTypeInformation {

  public static final String TYPE_IDENTIFIER = "Input";
  private final MetadataCacheId componentTypeMetadataCacheId;

  public InputMetadataResolutionTypeInformation(ComponentAst component, String parameterName) {
    super(component, (typeResolversInformationModelProperty -> getResolverInformation(typeResolversInformationModelProperty,
                                                                                      parameterName)));
    checkArgument(component.getModel(ParameterizedModel.class).isPresent(),
                  "Cannot generate an Input Cache Key for a component with no parameters");
    checkArgument(component.getModel(ParameterizedModel.class).get().getAllParameterModels().stream()
        .anyMatch(parameterModel -> parameterModel.getName().equals(parameterName)),
                  "Cannot generate an Input Cache Key for the component since it does not have a parameter named "
                      + parameterName);

    componentTypeMetadataCacheId = getTypeCacheId(parameterName);
  }

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
    componentTypeMetadataCacheId = getTypeCacheId(parameterName);
  }

  private MetadataCacheId getTypeCacheId(String parameterName) {
    String parameterTypeIdentifier;
    if (isDynamicType()) {
      parameterTypeIdentifier = TYPE_IDENTIFIER;
    } else {
      parameterTypeIdentifier = String.format("%s with parameter name : %s", TYPE_IDENTIFIER, parameterName);
    }
    return new MetadataCacheId(parameterTypeIdentifier.hashCode(), parameterTypeIdentifier);
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
