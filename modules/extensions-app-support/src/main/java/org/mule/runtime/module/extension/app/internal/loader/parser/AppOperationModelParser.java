/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.app.internal.loader.parser;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.type.ApplicationTypeLoader;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedChainModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedRouteModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class AppOperationModelParser extends BaseAppExtensionModelParser implements OperationModelParser {

  private final ComponentAst operation;
  private final ApplicationTypeLoader typeLoader;

  public AppOperationModelParser(ComponentAst operation, ApplicationTypeLoader typeLoader) {
    this.operation = operation;
    this.typeLoader = typeLoader;
  }

  private OutputModelParser asOutputModelParser(ComponentAst outputTypeElement) {
    String type = requiredString(outputTypeElement, "type");
    String mimeType = getMimeTypeFromOutputType(outputTypeElement).orElse("application/java");

    return typeLoader.load(type, mimeType)
        .map(mt -> new DefaultOutputModelParser(mt, false))
        .orElseThrow(() -> new IllegalModelDefinitionException(format(
            "Component <%s:%s> defines %s as '%s' with mediaType '%s', but such type is not defined in the application",
            outputTypeElement.getIdentifier().getNamespace(),
            outputTypeElement.getIdentifier().getName(),
            outputTypeElement.getIdentifier().getName(),
            mimeType)));
  }

  private Optional<String> getMimeTypeFromOutputType(ComponentAst outputTypeElement) {
    return optionalString(outputTypeElement, "mimeType");
  }

  @Override
  public String getName() {
    return requiredString(operation, "name");
  }

  @Override
  public String getDescription() {
    return optionalString(operation, "description").orElse("");
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return null;
  }

  @Override
  public List<String> getEmittedNotifications() {
    return emptyList();
  }

  @Override
  public OutputModelParser getOutputType() {
    return asOutputModelParser(getOutputPayloadTypeElement());
  }

  @Override
  public OutputModelParser getAttributesOutputType() {
    return asOutputModelParser(getOutputAttributesTypeElement());
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    ComponentParameterAst parametersElement = operation.getParameter(DEFAULT_GROUP_NAME, "parameters");
    if (parametersElement == null) {
      return emptyList();
    }

    List<ComponentAst> parameters = (List<ComponentAst>) parametersElement.getValue().getRight();
    return parameters.isEmpty()
        ? emptyList()
        : singletonList(new AppParameterGroupModelParser(parameters, typeLoader));
  }

  @Override
  public List<NestedRouteModelParser> getNestedRouteParsers() {
    return emptyList();
  }

  @Override
  public Optional<CompletableComponentExecutorModelProperty> getExecutorModelProperty() {
    return empty();
  }

  @Override
  public Optional<NestedChainModelParser> getNestedChainParser() {
    return empty();
  }

  @Override
  public boolean isBlocking() {
    return false;
  }

  @Override
  public boolean isIgnored() {
    return false;
  }

  @Override
  public boolean isScope() {
    return false;
  }

  @Override
  public boolean isRouter() {
    return false;
  }

  @Override
  public boolean isConnected() {
    return false;
  }

  @Override
  public boolean hasConfig() {
    return false;
  }

  @Override
  public boolean supportsStreaming() {
    return false;
  }

  @Override
  public boolean isTransactional() {
    return false;
  }

  @Override
  public boolean isAutoPaging() {
    return false;
  }

  @Override
  public Optional<ExecutionType> getExecutionType() {
    return of(CPU_LITE);
  }

  @Override
  public Optional<MediaTypeModelProperty> getMediaTypeModelProperty() {
    return getMimeTypeFromOutputType(getOutputPayloadTypeElement())
        .map(mimeType -> new MediaTypeModelProperty(mimeType, true));
  }

  @Override
  public Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty() {
    return empty();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return empty();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    String summary = optionalString(operation, "summary").orElse(null);
    String displayName = optionalString(operation, "displayName").orElse(null);

    if (!isBlank(displayName) || !isBlank(summary)) {
      return of(DisplayModel.builder()
          .summary(summary)
          .displayName(displayName)
          .build());
    }

    return empty();
  }

  @Override
  public List<ErrorModelParser> getErrorModelParsers() {
    return emptyList();
  }

  @Override
  public Set<String> getSemanticTerms() {
    return emptySet();
  }

  @Override
  public Optional<StereotypeModel> getStereotype(StereotypeModelFactory factory) {
    return empty();
  }

  private ComponentAst getOutputPayloadTypeElement() {
    return getOutputElement("payload-type");
  }

  private ComponentAst getOutputAttributesTypeElement() {
    return getOutputElement("attributes-type");
  }

  private ComponentAst getOutputElement(String elementName) {
    ComponentAst output = operation.directChildrenStreamByIdentifier(null, "output")
        .findFirst()
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
            "Operation '%s' is missing its <output> declaration", getName())));

    return output.directChildrenStreamByIdentifier(null, elementName)
        .findFirst()
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
            "Operation '%s' is missing its <%s> declaration", getName(), elementName)));
  }
}
