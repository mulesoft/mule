/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.getExceptionEnricherFactory;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getCompletionCallbackParameters;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConfigParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConnectionParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getOperationFieldParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.isInputStream;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forOperation;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getRoutes;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.NestedChainModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedRouteModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.List;
import java.util.Optional;

public class JavaOperationModelParser extends AbstractExecutableComponentModelParser implements OperationModelParser {

  private static final List<Class<?>> ROUTER_CALLBACK_PARAMETER_TYPES = asList(
                                                                               RouterCompletionCallback.class,
                                                                               org.mule.sdk.api.runtime.process.RouterCompletionCallback.class,
                                                                               VoidCompletionCallback.class,
                                                                               org.mule.sdk.api.runtime.process.VoidCompletionCallback.class);

  private final OperationElement operationElement;
  private final OperationContainerElement operationContainer;
  private final OperationContainerElement enclosingType;

  private final Optional<ExtensionParameter> configParameter;
  private final Optional<ExtensionParameter> connectionParameter;

  private ExtensionParameter nestedChain;
  private boolean blocking = false;
  private boolean scope = false;
  private boolean router = false;
  private boolean autoPaging = false;
  private List<ExtensionParameter> routes = emptyList();

  public JavaOperationModelParser(ExtensionElement extensionElement,
                                  OperationContainerElement operationContainer,
                                  OperationElement operationElement,
                                  ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);

    this.operationElement = operationElement;

    this.operationContainer = operationElement.getEnclosingType();
    enclosingType = operationContainer != null ? operationContainer : this.operationContainer;
    checkOperationIsNotAnExtension();

    configParameter = getConfigParameter(operationElement);
    connectionParameter = getConnectionParameter(operationElement);

    parseStructure();
    collectAdditionalModelProperties();
  }

  private void parseStructure() {
    final List<ExtensionParameter> callbackParameters = getCompletionCallbackParameters(operationElement);
    blocking = callbackParameters.isEmpty();
    connected = connectionParameter.isPresent();
    nestedChain = fetchNestedChain();
    scope = nestedChain != null;
    routes = getRoutes(operationElement);
    router = !routes.isEmpty();

    if (scope && router) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Operation '%s' is both a Scope and a Router, which is invalid",
                                                                getName()));
    }

    if (blocking) {
      parseBlockingOperation();
    } else {
      parseNonBlockingOperation(callbackParameters);
    }

    if (scope) {
      validateScope();
    }
    if (router) {
      parseRouter();
    }
  }

  private ExtensionParameter fetchNestedChain() {
    List<ExtensionParameter> chains =
        operationElement.getParameters().stream().filter(JavaExtensionModelParserUtils::isProcessorChain).collect(toList());

    if (chains.size() > 1) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Scope '%s' declares too many parameters of type '%s', only one input of this kind is supported."
                                                             + "Offending parameters are: %s",
                                                                getName(),
                                                                Chain.class.getSimpleName(),
                                                                chains.stream().map(ExtensionParameter::getName)
                                                                    .collect(toList())));
    }

    return chains.isEmpty() ? null : chains.get(0);
  }

  private void validateScope() {
    if (blocking) {
      throw new IllegalOperationModelDefinitionException(format("Scope '%s' does not declare a '%s' parameter. One is required " +
          "for all operations that receive and execute a Chain of other components",
                                                                getName(),
                                                                CompletionCallback.class.getSimpleName()));
    }

    if (hasConfig()) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Scope '%s' requires a config, but that is not allowed, remove such parameter",
                                                                getName()));
    }

    if (isConnected()) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Scope '%s' requires a connection, but that is not allowed, remove such parameter",
                                                                getName()));
    }
  }

  private void parseRouter() {
    if (hasConfig()) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' requires a config, but that is not allowed, remove such parameter",
                                                                getName()));
    }

    if (isConnected()) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' requires a connection, but that is not allowed, remove such parameter",
                                                                getName()));
    }

    List<ExtensionParameter> callbackParameters = operationElement.getParameters().stream()
        .filter(this::isRouterCallback)
        .collect(toList());

    if (callbackParameters.isEmpty()) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' does not declare a parameter with one of the types '%s'. One is required.",
                                                                getName(), ROUTER_CALLBACK_PARAMETER_TYPES));
    } else if (callbackParameters.size() > 1) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' defines more than one CompletionCallback parameters. Only one is allowed",
                                                                getName()));
    }

    List<ExtensionParameter> routes = operationElement.getParameters().stream().filter(this::isRoute).collect(toList());

    if (routes.isEmpty()) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' does not declare a '%s' parameter. One is required.",
                                                                getName(), Route.class.getSimpleName()));
    }

    if (!IntrospectionUtils.isVoid(operationElement)) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' is not declared in a void method.", getName()));
    }
  }

  private boolean isRouterCallback(ExtensionParameter p) {
    return ROUTER_CALLBACK_PARAMETER_TYPES.stream().anyMatch(type -> p.getType().isSameType(type));
  }

  private boolean isRoute(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Route.class);
  }

  private void parseBlockingOperation() {
    // TODO: Should be possible to parse dynamic types right here
    outputType = new DefaultOutputModelParser(operationElement.getReturnMetadataType(), false);
    outputAttributesType = new DefaultOutputModelParser(operationElement.getAttributesMetadataType(), false);

    processComponentConnectivity(operationElement);

    if (autoPaging = JavaExtensionModelParserUtils.isAutoPaging(operationElement)) {
      parseAutoPaging();
    } else {
      supportsStreaming = isInputStream(outputType.getType())
          || operationElement.getAnnotation(Streaming.class).isPresent()
          || operationElement.getAnnotation(org.mule.sdk.api.annotation.Streaming.class).isPresent();
    }
  }

  private void parseAutoPaging() {
    supportsStreaming = true;
    connected = true;
    additionalModelProperties.add(new PagedOperationModelProperty());
    parsePagingTx();
  }

  private void parsePagingTx() {
    Type returnTypeElement = operationElement.getReturnType();
    List<TypeGeneric> generics = returnTypeElement.getGenerics();

    if (!generics.isEmpty()) {
      transactional = generics.get(0).getConcreteType().isAssignableTo(TransactionalConnection.class);
    } else {
      transactional = false;
    }
  }

  private void parseNonBlockingOperation(List<ExtensionParameter> callbackParameters) {
    if (callbackParameters.size() > 1) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Operation '%s' defines more than one %s parameters. Only one is allowed",
                                                                getName(), CompletionCallback.class.getSimpleName()));
    }

    if (!isVoid(operationElement)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Operation '%s' has a parameter of type %s but is not void. "
                                                             + "Non-blocking operations have to be declared as void and the "
                                                             + "return type provided through the callback",
                                                                getName(),
                                                                CompletionCallback.class.getSimpleName()));
    }

    ExtensionParameter callbackParameter = callbackParameters.get(0);

    List<MetadataType> genericTypes = callbackParameter.getType()
        .getGenerics()
        .stream()
        .map(generic -> generic.getConcreteType().asMetadataType())
        .collect(toList());

    if (genericTypes.isEmpty()) {
      // This is an invalid state, but is better to fail when executing the Extension Model Validators
      MetadataType anyType = PRIMITIVE_TYPES.get("ANY");
      genericTypes.add(anyType);
      genericTypes.add(anyType);
    }

    // TODO: SHould be possible to parse dynamic types right here?
    outputType = new DefaultOutputModelParser(genericTypes.get(0), false);
    outputAttributesType = new DefaultOutputModelParser(genericTypes.get(1), false);
  }

  @Override
  public String getName() {
    return operationElement.getAlias();
  }

  @Override
  public String getDescription() {
    return operationElement.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    List<ExtensionParameter> methodParameters;

    if (isRouter()) {
      methodParameters = operationElement.getParameters().stream()
          .filter(p -> !isRoute(p) && !isRouterCallback(p))
          .collect(toList());
    } else {
      methodParameters = operationElement.getParameters();
    }

    ParameterDeclarationContext context = forOperation(getName());

    List<ParameterGroupModelParser> parameterGroupModelParsers = getParameterGroupParsers(methodParameters, context);
    parameterGroupModelParsers.addAll(getOperationFieldParameterGroupParsers(operationContainer.getParameters(), context));

    return parameterGroupModelParsers;
  }

  @Override
  public List<NestedRouteModelParser> getNestedRouteParsers() {
    return routes.stream().map(r -> new JavaNestedRouteModelParser(r)).collect(toList());
  }

  @Override
  public Optional<NestedChainModelParser> getNestedChainParser() {
    return nestedChain != null ? of(new JavaNestedChainModelParser(nestedChain)) : empty();
  }

  @Override
  public CompletableComponentExecutorModelProperty getExecutorModelProperty() {
    return new CompletableComponentExecutorModelProperty(
                                                         new CompletableOperationExecutorFactory(enclosingType.getDeclaringClass()
                                                             .get(), operationElement.getMethod().get()));
  }

  @Override
  public boolean isIgnored() {
    return IntrospectionUtils.isIgnored(operationElement, loadingContext);
  }

  @Override
  public OutputModelParser getOutputType() {
    return outputType;
  }

  @Override
  public OutputModelParser getAttributesOutputType() {
    return outputAttributesType;
  }

  @Override
  public boolean supportsStreaming() {
    return supportsStreaming;
  }

  @Override
  public boolean isScope() {
    return scope;
  }

  @Override
  public boolean isRouter() {
    return router;
  }

  @Override
  public boolean isConnected() {
    return connected;
  }

  @Override
  public boolean isTransactional() {
    return transactional;
  }

  @Override
  public boolean isAutoPaging() {
    return autoPaging;
  }

  @Override
  public Optional<ExecutionType> getExecutionType() {
    return operationElement.getAnnotation(Execution.class).map(Execution::value);
  }

  @Override
  public boolean isNonBlocking() {
    return !blocking;
  }

  @Override
  public Optional<MediaTypeModelProperty> getMediaTypeModelProperty() {
    return operationElement.getAnnotation(MediaType.class)
        .map(a -> new MediaTypeModelProperty(a.value(), a.strict()));
  }

  @Override
  public Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty() {
    return getExceptionEnricherFactory(operationElement).map(ExceptionHandlerModelProperty::new);
  }

  @Override
  public boolean isBlocking() {
    return blocking;
  }

  @Override
  public boolean hasConfig() {
    return configParameter.isPresent();
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return additionalModelProperties;
  }

  private void checkOperationIsNotAnExtension() {
    if (operationContainer.isAssignableFrom(extensionElement) || extensionElement.isAssignableFrom(operationContainer)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                operationContainer.getName(), extensionElement.getName()));
    }
  }

  private void collectAdditionalModelProperties() {
    additionalModelProperties.add(new ExtensionOperationDescriptorModelProperty(operationElement));
    additionalModelProperties.add(new ImplementingMethodModelProperty(operationElement.getMethod().get()));
  }

  @Override
  protected String getComponentTypeName() {
    if (isScope()) {
      return "Scope";
    } else if (isRouter()) {
      return "Construct";
    }
    return "Operation";
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof JavaOperationModelParser) {
      return operationElement.equals(((JavaOperationModelParser) o).operationElement);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(operationElement);
  }
}
