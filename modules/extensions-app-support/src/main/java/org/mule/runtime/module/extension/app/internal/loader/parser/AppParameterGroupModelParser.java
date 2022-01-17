/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.app.internal.loader.parser;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.type.ApplicationTypeLoader;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.module.extension.internal.loader.java.property.ExclusiveOptionalModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;

import java.util.List;
import java.util.Optional;

class AppParameterGroupModelParser implements ParameterGroupModelParser {

  private final List<ComponentAst> parameters;
  private final ApplicationTypeLoader typeLoader;

  public AppParameterGroupModelParser(List<ComponentAst> parameters, ApplicationTypeLoader typeLoader) {
    this.parameters = parameters;
    this.typeLoader = typeLoader;
  }

  @Override
  public String getName() {
    return DEFAULT_GROUP_NAME;
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public List<ParameterModelParser> getParameterParsers() {
    if (parameters != null) {
      parameters.forEach(p -> declareParameter(declarer, p));
    }
  }

  private void declareParameter(ParameterizedDeclarer declarer, ComponentAst parameterAst) {
    final String paramName = requiredString(parameterAst, "name");
    ParameterDeclarer parameter;

    ComponentParameterAst optionalElement = parameterAst.getParameter("optional");

    if (optionalElement != null) {
      ComponentAst optional = (ComponentAst) optionalElement.getValue().getRight();
      parameter = declarer.onDefaultParameterGroup().withOptionalParameter(paramName)
          .defaultingTo(optionalString(optional, "defaultValue"));

      ComponentParameterAst exclusiveOptional = optional.getParameter("exclusiveOptional");
      if (exclusiveOptional != null) {
        parameter.withModelProperty(new ExclusiveOptionalModelProperty(parseExclusiveParametersModel((ComponentAst) exclusiveOptional.getValue().getRight())));
      }
    } else {
      parameter = declarer.onDefaultParameterGroup().withRequiredParameter(paramName);
    }

    parameter.describedAs(optionalString(parameterAst, "description"))
        .ofType(typeLoader.load())
        .withExpressionSupport()
        .withRole(BEHAVIOUR);
  }


  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return empty();
  }

  @Override
  public Optional<LayoutModel> getLayoutModel() {
    return empty();
  }

  @Override
  public Optional<ExclusiveOptionalDescriptor> getExclusiveOptionals() {
    return empty();
  }

  @Override
  public boolean showsInDsl() {
    return false;
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return emptyList();
  }
}
