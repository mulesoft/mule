/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.api.types;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.extension.api.component.ComponentParameterization;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;

import java.util.Optional;

/**
 * Implementation of {@link MetadataResolutionTypeInformation} that describes Input Types from a {@link ComponentParameterization}
 *
 * @since 4.5
 */
public class ComponentParameterizationInputMetadataResolutionTypeInformation extends AbstractMetadataResolutionTypeInformation {

  public static final String TYPE_IDENTIFIER = "Input";
  private final MetadataCacheId componentTypeMetadataCacheId;

  public ComponentParameterizationInputMetadataResolutionTypeInformation(ComponentParameterization parameterization,
                                                                         String parameterName) {
    super(parameterization,
          (typeResolversInformationModelProperty -> getResolverInformation(typeResolversInformationModelProperty,
                                                                           parameterName)));
    componentTypeMetadataCacheId = getTypeCacheId(parameterName, empty());
  }

  public ComponentParameterizationInputMetadataResolutionTypeInformation(ComponentParameterization parameterization,
                                                                         String parameterGroupName,
                                                                         String parameterName) {
    super(parameterization,
          (typeResolversInformationModelProperty -> getResolverInformation(typeResolversInformationModelProperty,
                                                                           parameterName)));
    componentTypeMetadataCacheId = getTypeCacheId(parameterName, of(parameterGroupName));
  }

  private MetadataCacheId getTypeCacheId(String parameterName, Optional<String> parameterGroupName) {
    String parameterGroupIdentifier = parameterGroupName.map(pgn -> pgn + ":").orElse("");

    String parameterTypeIdentifier;
    if (isDynamicType()) {
      parameterTypeIdentifier = parameterGroupIdentifier + TYPE_IDENTIFIER;
    } else {
      parameterTypeIdentifier = format("%s with parameter name : %s%s", TYPE_IDENTIFIER, parameterGroupIdentifier, parameterName);
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

