/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.util.List;
import java.util.Optional;

public class JavaDefaultParameterGroupParser implements ParameterGroupModelParser {

  private final List<ExtensionParameter> parameters;
  private final ClassTypeLoader typeLoader;
  private final ParameterDeclarationContext context;

  public JavaDefaultParameterGroupParser(List<ExtensionParameter> parameters,
                                         ClassTypeLoader typeLoader,
                                         ParameterDeclarationContext context) {
    this.parameters = parameters;
    this.typeLoader = typeLoader;
    this.context = context;
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
    return unmodifiableList(parameters.stream()
        .map(p -> new JavaParameterModelParser(p, typeLoader, context))
        .collect(toList()));
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
