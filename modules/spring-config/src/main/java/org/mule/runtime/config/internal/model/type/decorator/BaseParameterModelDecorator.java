/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type.decorator;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

import java.util.List;
import java.util.Optional;
import java.util.Set;


public abstract class BaseParameterModelDecorator implements ParameterModel {

  private final ParameterModel decorated;

  public BaseParameterModelDecorator(ParameterModel decorated) {
    this.decorated = decorated;
  }

  public ParameterModel getDecorated() {
    return decorated;
  }

  @Override
  public String getName() {
    return decorated.getName();
  }

  @Override
  public String getDescription() {
    return decorated.getDescription();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return decorated.getDeprecationModel();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return decorated.getDisplayModel();
  }

  @Override
  public MetadataType getType() {
    return decorated.getType();
  }

  @Override
  public boolean hasDynamicType() {
    return decorated.hasDynamicType();
  }

  @Override
  public boolean isDeprecated() {
    return decorated.isDeprecated();
  }

  @Override
  public <T extends ModelProperty> Optional<T> getModelProperty(Class<T> propertyType) {
    return decorated.getModelProperty(propertyType);
  }

  @Override
  public boolean isRequired() {
    return decorated.isRequired();
  }

  @Override
  public boolean isOverrideFromConfig() {
    return decorated.isOverrideFromConfig();
  }

  @Override
  public Set<ModelProperty> getModelProperties() {
    return decorated.getModelProperties();
  }

  @Override
  public ExpressionSupport getExpressionSupport() {
    return decorated.getExpressionSupport();
  }

  @Override
  public Object getDefaultValue() {
    return decorated.getDefaultValue();
  }

  @Override
  public ParameterDslConfiguration getDslConfiguration() {
    return decorated.getDslConfiguration();
  }

  @Override
  public ParameterRole getRole() {
    return decorated.getRole();
  }

  @Override
  public Optional<LayoutModel> getLayoutModel() {
    return decorated.getLayoutModel();
  }

  @Override
  public List<StereotypeModel> getAllowedStereotypes() {
    return decorated.getAllowedStereotypes();
  }

  @Override
  public Optional<ValueProviderModel> getValueProviderModel() {
    return decorated.getValueProviderModel();
  }

  @Override
  public boolean isComponentId() {
    return decorated.isComponentId();
  }

}
