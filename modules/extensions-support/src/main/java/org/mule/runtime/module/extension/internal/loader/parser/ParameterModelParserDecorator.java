/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.extension.api.loader.parser.MinMuleVersionParser;
import org.mule.runtime.extension.api.loader.parser.ParameterModelParser;
import org.mule.runtime.extension.api.loader.parser.StereotypeModelFactory;
import org.mule.runtime.extension.api.loader.parser.metadata.InputResolverModelParser;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Base class for implementing the decorator pattern around a {@link ParameterModelParser}
 */
public class ParameterModelParserDecorator implements ParameterModelParser {

  protected final ParameterModelParser decoratee;

  /**
   * Creates a new instance
   *
   * @param decoratee the decorated instance
   */
  public ParameterModelParserDecorator(ParameterModelParser decoratee) {
    this.decoratee = decoratee;
  }

  @Override
  public String getName() {
    return decoratee.getName();
  }

  @Override
  public String getDescription() {
    return decoratee.getDescription();
  }

  @Override
  public MetadataType getType() {
    return decoratee.getType();
  }

  @Override
  public boolean isRequired() {
    return decoratee.isRequired();
  }

  @Override
  public Object getDefaultValue() {
    return decoratee.getDefaultValue();
  }

  @Override
  public ParameterRole getRole() {
    return decoratee.getRole();
  }

  @Override
  public ExpressionSupport getExpressionSupport() {
    return decoratee.getExpressionSupport();
  }

  @Override
  public Optional<LayoutModel> getLayoutModel() {
    return decoratee.getLayoutModel();
  }

  @Override
  public Optional<ParameterDslConfiguration> getDslConfiguration() {
    return decoratee.getDslConfiguration();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return decoratee.getDeprecationModel();
  }

  @Override
  public boolean isExcludedFromConnectivitySchema() {
    return decoratee.isExcludedFromConnectivitySchema();
  }

  @Override
  public boolean isConfigOverride() {
    return decoratee.isConfigOverride();
  }

  @Override
  public boolean isComponentId() {
    return decoratee.isComponentId();
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return decoratee.getAdditionalModelProperties();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return decoratee.getDisplayModel();
  }

  @Override
  public Set<String> getSemanticTerms() {
    return decoratee.getSemanticTerms();
  }

  @Override
  public List<StereotypeModel> getAllowedStereotypes(StereotypeModelFactory factory) {
    return decoratee.getAllowedStereotypes(factory);
  }

  @Override
  public Optional<OAuthParameterModelProperty> getOAuthParameterModelProperty() {
    return decoratee.getOAuthParameterModelProperty();
  }

  @Override
  public Optional<MinMuleVersionParser> getResolvedMinMuleVersion() {
    return decoratee.getResolvedMinMuleVersion();
  }

  @Override
  public Optional<InputResolverModelParser> getInputResolverModelParser() {
    return decoratee.getInputResolverModelParser();
  }


  @Override
  public Optional<Pair<Integer, Boolean>> getMetadataKeyPart() {
    return decoratee.getMetadataKeyPart();
  }

  public ParameterModelParser getDecoratee() {
    return decoratee;
  }
}
