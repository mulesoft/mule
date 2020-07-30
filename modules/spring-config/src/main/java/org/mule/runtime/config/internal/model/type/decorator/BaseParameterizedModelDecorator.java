/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type.decorator;

import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;

import java.util.List;

public abstract class BaseParameterizedModelDecorator implements ParameterizedModel {

  private final ParameterizedModel decorated;

  public BaseParameterizedModelDecorator(ParameterizedModel decorated) {
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
  public List<ParameterGroupModel> getParameterGroupModels() {
    return decorated.getParameterGroupModels();
  }

  @Override
  public List<ParameterModel> getAllParameterModels() {
    return decorated.getAllParameterModels();
  }


}
