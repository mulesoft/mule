/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

import java.util.List;
import java.util.Optional;

public class ParameterModelParserDecorator implements ParameterModelParser{

  protected final ParameterModelParser decoratee;

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
  public List<StereotypeModel> getAllowedStereotypes() {
    return decoratee.getAllowedStereotypes();
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
}
