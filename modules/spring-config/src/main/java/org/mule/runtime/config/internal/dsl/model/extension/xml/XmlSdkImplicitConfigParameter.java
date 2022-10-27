/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import static java.util.Optional.empty;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.ast.api.ComponentGenerationInformation.EMPTY_GENERATION_INFO;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.Optional;

/**
 * AST component that represent a parameter of a configuration intended to be used when there is not a configuration on the
 * application but there are modules from the XML Sdk that have XML Sdk properties with default values.
 */
public class XmlSdkImplicitConfigParameter implements ComponentParameterAst {

  private final ParameterModel parameterModel;
  private final Object value;


  public XmlSdkImplicitConfigParameter(ParameterModel parameterModel, Object value) {
    this.parameterModel = parameterModel;
    this.value = value;
  }

  @Override
  public ParameterModel getModel() {
    return parameterModel;
  }

  @Override
  public ParameterGroupModel getGroupModel() {
    return null;
  }

  @Override
  public Either<String, Object> getValue() {
    return right(value);
  }

  @Override
  public String getRawValue() {
    return value != null ? value.toString() : null;
  }

  @Override
  public String getResolvedRawValue() {
    return value != null ? value.toString() : null;
  }

  @Override
  public Optional<ComponentMetadataAst> getMetadata() {
    return empty();
  }

  @Override
  public ComponentGenerationInformation getGenerationInformation() {
    return EMPTY_GENERATION_INFO;
  }

  @Override
  public boolean isDefaultValue() {
    return true;
  }
}
