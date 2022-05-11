/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ParameterModelParser} implementation for Mule SDK (required parameters).
 *
 * @since 4.5.0
 */
public class MuleSdkParameterModelParserSdk extends BaseMuleSdkExtensionModelParser implements ParameterModelParser {

  protected final ComponentAst parameter;
  private final TypeLoader typeLoader;

  private String name;

  public MuleSdkParameterModelParserSdk(ComponentAst parameter, TypeLoader typeLoader) {
    this.parameter = parameter;
    this.typeLoader = typeLoader;

    parseStructure();
  }

  private void parseStructure() {
    name = getParameter(parameter, "name");
  }

  @Override
  public List<StereotypeModel> getAllowedStereotypes(StereotypeModelFactory factory) {
    return emptyList();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return this.<String>getOptionalParameter(parameter, "description").orElse("");
  }

  @Override
  public MetadataType getType() {
    final String type = getParameter(parameter, "type");
    return typeLoader.load(type).orElseThrow(() -> new IllegalModelDefinitionException(
                                                                                       format("Parameter '%s' references unknown type '%s'",
                                                                                              getName(), type)));
  }

  @Override
  public boolean isRequired() {
    return true;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

  @Override
  public ParameterRole getRole() {
    return BEHAVIOUR;
  }

  @Override
  public ExpressionSupport getExpressionSupport() {
    return ExpressionSupport.valueOf(getParameter(parameter, "expressionSupport"));
  }

  @Override
  public Optional<LayoutModel> getLayoutModel() {
    return empty();
  }

  @Override
  public Optional<ParameterDslConfiguration> getDslConfiguration() {
    return empty();
  }

  @Override
  public boolean isExcludedFromConnectivitySchema() {
    return false;
  }

  @Override
  public boolean isConfigOverride() {
    return getParameter(parameter, "configOverride");
  }

  @Override
  public boolean isComponentId() {
    return false;
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return emptyList();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return empty();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    String summary = getParameter(parameter, "summary");
    if (!isBlank(summary)) {
      return of(DisplayModel.builder().summary(summary).build());
    }
    return empty();
  }

  @Override
  public Optional<OAuthParameterModelProperty> getOAuthParameterModelProperty() {
    return empty();
  }

  @Override
  public Set<String> getSemanticTerms() {
    return emptySet();
  }
}
