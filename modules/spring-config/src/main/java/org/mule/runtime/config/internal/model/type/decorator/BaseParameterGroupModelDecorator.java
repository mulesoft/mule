/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type.decorator;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class BaseParameterGroupModelDecorator implements ParameterGroupModel {

  private final ParameterGroupModel decorated;

  public BaseParameterGroupModelDecorator(ParameterGroupModel decorated) {
    this.decorated = decorated;
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
  public Optional<LayoutModel> getLayoutModel() {
    return decorated.getLayoutModel();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return decorated.getDisplayModel();
  }

  @Override
  public <T extends ModelProperty> Optional<T> getModelProperty(Class<T> propertyType) {
    return decorated.getModelProperty(propertyType);
  }

  @Override
  public List<ParameterModel> getParameterModels() {
    return decorated.getParameterModels();
  }

  @Override
  public List<ExclusiveParametersModel> getExclusiveParametersModels() {
    return decorated.getExclusiveParametersModels();
  }

  @Override
  public Set<ModelProperty> getModelProperties() {
    return decorated.getModelProperties();
  }

  @Override
  public Optional<ParameterModel> getParameter(String name) {
    return decorated.getParameter(name);
  }

  @Override
  public boolean isShowInDsl() {
    return decorated.isShowInDsl();
  }


}
