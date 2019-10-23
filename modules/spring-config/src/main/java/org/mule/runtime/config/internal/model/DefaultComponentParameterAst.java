/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.Optional;
import java.util.function.Supplier;


public class DefaultComponentParameterAst implements ComponentParameterAst {

  private final String rawValue;
  private final Supplier<ParameterModel> model;
  private final ComponentMetadataAst metadata;

  public DefaultComponentParameterAst(String rawValue, Supplier<ParameterModel> model) {
    this(rawValue, model, null);
  }

  public DefaultComponentParameterAst(String rawValue, Supplier<ParameterModel> model, ComponentMetadataAst metadata) {
    this.rawValue = rawValue;
    this.model = model;
    this.metadata = metadata;
  }

  @Override
  public Object getValue() {
    // previous implementations were assuming that an empty string was the same as the param not being present...
    return isEmpty(rawValue) ? getModel().getDefaultValue() : rawValue;
  }

  @Override
  public String getRawValue() {
    return rawValue;
  }

  @Override
  public ParameterModel getModel() {
    return model.get();
  }

  @Override
  public Optional<ComponentMetadataAst> getMetadata() {
    return ofNullable(metadata);
  }

}
