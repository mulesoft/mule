/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterModelParser;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link ParameterGroupModelParser} implementation for Mule SDK
 *
 * @since 4.5.0
 */
class MuleSdkParameterGroupModelParser extends BaseMuleSdkExtensionModelParser implements ParameterGroupModelParser {

  private final List<ParameterModelParser> parameters;
  private final Optional<ExclusiveOptionalDescriptor> exclusiveOptionals;
  private final ExtensionModelHelper extensionModelHelper;

  public MuleSdkParameterGroupModelParser(ComponentAst parametersComponent, TypeLoader typeLoader,
                                          ExtensionModelHelper extensionModelHelper) {
    this.extensionModelHelper = extensionModelHelper;
    parameters = doParserParameters(parametersComponent, typeLoader);
    exclusiveOptionals = doParseExclusiveOptionalDescriptorFromGroup(parametersComponent);
  }

  private List<ParameterModelParser> doParserParameters(ComponentAst parametersComponent, TypeLoader typeLoader) {
    Stream<ParameterModelParser> parameterParsers = getChildren(parametersComponent, "parameter")
        .map(p -> new MuleSdkParameterModelParser(p, typeLoader, extensionModelHelper));
    Stream<ParameterModelParser> optionalParameterParsers = getChildren(parametersComponent, "optional-parameter")
        .map(p -> new MuleSdkOptionalParameterModelParser(p, typeLoader, extensionModelHelper));

    return concat(parameterParsers, optionalParameterParsers).collect(toList());
  }

  private Optional<ExclusiveOptionalDescriptor> doParseExclusiveOptionalDescriptorFromGroup(ComponentAst parametersComponent) {
    return getSingleChild(parametersComponent, "exclusive-optionals")
        .map(exclusiveOptionals -> doParseExclusiveOptionalDescriptor(exclusiveOptionals));
  }

  private ExclusiveOptionalDescriptor doParseExclusiveOptionalDescriptor(ComponentAst exclusiveOptionals) {
    Set<String> parameters = Stream.of(this.<String>getParameter(exclusiveOptionals, "exclusiveOptionals").split(","))
        .map(String::trim)
        .filter(p -> !isBlank(p))
        .collect(toCollection(LinkedHashSet::new));

    return new ExclusiveOptionalDescriptor(parameters,
                                           (boolean) getOptionalParameter(exclusiveOptionals, "oneRequired").orElse(false));
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
    return parameters;
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
    return exclusiveOptionals;
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
