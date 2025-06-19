/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.ComponentVisibility.PUBLIC;
import static org.mule.runtime.extension.privileged.semantic.SemanticTermsHelper.getAllTermsFromAnnotations;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getCompletionCallbackParameters;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConfigParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConnectionParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forOperation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParserUtils.parseOperationErrorModels;
import static org.mule.runtime.module.extension.internal.loader.parser.java.semantics.SemanticTermsParserUtils.addCustomTerms;
import static org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils.resolveStereotype;
import static org.mule.runtime.module.extension.internal.loader.parser.java.type.CustomStaticTypeUtils.getOperationAttributesType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.type.CustomStaticTypeUtils.getOperationOutputType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.getContainerAnnotationMinMuleVersion;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.resolveOperationMinMuleVersion;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaInputResolverModelParserUtils.parseInputResolversModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils.getKeyIdResolverModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils.parseKeyIdResolverModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.getRoutes;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.isRoute;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.parseAttributesResolverModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.parseOutputResolverModelParser;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ComponentVisibility;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.parser.MediaTypeParser;
import org.mule.runtime.extension.api.loader.parser.MinMuleVersionParser;
import org.mule.runtime.extension.api.runtime.exception.SdkExceptionHandlerFactory;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.extension.api.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.extension.api.loader.parser.ErrorModelParser;
import org.mule.runtime.extension.api.loader.parser.NestedChainModelParser;
import org.mule.runtime.extension.api.loader.parser.NestedRouteModelParser;
import org.mule.runtime.extension.api.loader.parser.OperationModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParserDecorator;
import org.mule.runtime.extension.api.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.connection.JavaConnectionProviderModelParserUtils;
import org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParserUtils;
import org.mule.runtime.module.extension.internal.loader.parser.java.metadata.JavaRoutesChainInputTypesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.metadata.JavaScopeChainInputTypeResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils;
import org.mule.runtime.extension.api.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.OutputResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.RoutesChainInputTypesResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.ScopeChainInputTypeResolverModelParser;
import org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.sdk.api.annotation.Operations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * {@link OperationModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaOperationModelParser extends AbstractJavaExecutableComponentModelParser implements OperationModelParser {

  private static final List<Class<?>> ROUTER_CALLBACK_PARAMETER_TYPES = asList(
                                                                               RouterCompletionCallback.class,
                                                                               org.mule.sdk.api.runtime.process.RouterCompletionCallback.class,
                                                                               VoidCompletionCallback.class,
                                                                               org.mule.sdk.api.runtime.process.VoidCompletionCallback.class,
                                                                               CompletionCallback.class,
                                                                               org.mule.sdk.api.runtime.process.CompletionCallback.class);

  private final JavaExtensionModelParser extensionModelParser;
  private final OperationElement operationElement;
  private final OperationContainerElement operationContainer;
  private final OperationContainerElement enclosingType;

  private final Optional<ExtensionParameter> configParameter;
  private final Optional<ExtensionParameter> connectionParameter;

  private ExtensionParameter nestedChain;
  private boolean blocking = false;
  private boolean scope = false;
  private boolean router = false;
  private boolean hasDeprecatedRouterCompletion = false;
  private boolean autoPaging = false;
  private List<ExtensionParameter> routes = emptyList();

  private Optional<MetadataKeyModelParser> metadataKeyModelParser;

  public JavaOperationModelParser(JavaExtensionModelParser extensionModelParser,
                                  ExtensionElement extensionElement,
                                  OperationContainerElement operationContainer,
                                  OperationElement operationElement,
                                  ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);
    this.extensionModelParser = extensionModelParser;
    this.operationElement = operationElement;
    configParameter = getConfigParameter(operationElement);
    if (!isIgnored()) {
      this.operationContainer = operationElement.getEnclosingType();
      enclosingType = operationContainer != null ? operationContainer : this.operationContainer;
      checkOperationIsNotAnExtension();

      connectionParameter = getConnectionParameter(operationElement);

      parseStructure();
      collectAdditionalModelProperties();
    } else {
      this.operationContainer = null;
      enclosingType = null;
      connectionParameter = null;
    }
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

    parseComponentConnectivity(operationElement);

    if (blocking) {
      parseBlockingOperation();
    } else {
      parseNonBlockingOperation(callbackParameters);
    }

    if (!autoPaging) {
      parseComponentByteStreaming(operationElement);
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

    if (routes.isEmpty()) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' does not declare a '%s' parameter. One is required.",
                                                                getName(), Route.class.getSimpleName()));
    }

    if (!isVoid(operationElement)) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Router '%s' is not declared in a void method.", getName()));
    }
    hasDeprecatedRouterCompletion =
        operationElement.getParameters().stream().anyMatch(this::parameterOfRouterCompletionCallbackType);
  }

  private boolean isRouterCallback(ExtensionParameter p) {
    return ROUTER_CALLBACK_PARAMETER_TYPES.stream().anyMatch(type -> p.getType().isSameType(type));
  }

  private void parseBlockingOperation() {
    Optional<OutputResolverModelParser> outputResolverModelParser = getOutputResolverModelParser();
    boolean isDynamicResolver = outputResolverModelParser.isPresent() && outputResolverModelParser.get().hasOutputResolver();
    outputType = new DefaultOutputModelParser(getOperationOutputType(operationElement), isDynamicResolver);

    Optional<AttributesResolverModelParser> attributesResolverModelParser = getAttributesResolverModelParser();
    isDynamicResolver = attributesResolverModelParser.isPresent() && attributesResolverModelParser.get().hasAttributesResolver();
    outputAttributesType = new DefaultOutputModelParser(getOperationAttributesType(operationElement), isDynamicResolver);

    autoPaging = JavaExtensionModelParserUtils.isAutoPaging(operationElement);
    if (autoPaging) {
      parseAutoPaging();
    }
  }

  private void parseAutoPaging() {
    supportsStreaming = true;
    connected = true;
    parsePagingTx();
  }

  private void parsePagingTx() {
    Type returnTypeElement = operationElement.getReturnType();
    List<TypeGeneric> generics = returnTypeElement.getGenerics();

    if (!generics.isEmpty()) {
      transactional = JavaConnectionProviderModelParserUtils.isTransactional(generics.get(0).getConcreteType());
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
    Optional<OutputResolverModelParser> outputResolverModelParser = getOutputResolverModelParser();
    boolean isDynamicResolver = outputResolverModelParser.isPresent() && outputResolverModelParser.get().hasOutputResolver();
    outputType = new DefaultOutputModelParser(getOperationOutputType(operationElement), isDynamicResolver);

    Optional<AttributesResolverModelParser> attributesResolverModelParser = getAttributesResolverModelParser();
    isDynamicResolver = attributesResolverModelParser.isPresent() && attributesResolverModelParser.get().hasAttributesResolver();
    outputAttributesType = new DefaultOutputModelParser(getOperationAttributesType(operationElement), isDynamicResolver);
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

    ParameterDeclarationContext context = forOperation(getName(), loadingContext, hasKeyResolverAvailable());

    List<ParameterGroupModelParser> parameterGroupModelParsers = getParameterGroupParsers(methodParameters, context);
    parameterGroupModelParsers.addAll(
                                      getParameterGroupParsers(operationContainer.getParameters(),
                                                               context,
                                                               p -> new ParameterModelParserDecorator(p) {

                                                                 @Override
                                                                 public ExpressionSupport getExpressionSupport() {
                                                                   return NOT_SUPPORTED;
                                                                 }

                                                                 @Override
                                                                 public List<ModelProperty> getAdditionalModelProperties() {
                                                                   List<ModelProperty> modelProperties =
                                                                       decoratee.getAdditionalModelProperties();
                                                                   modelProperties
                                                                       .add(new FieldOperationParameterModelProperty());

                                                                   return modelProperties;
                                                                 }
                                                               }));

    return parameterGroupModelParsers;
  }

  @Override
  public List<NestedRouteModelParser> getNestedRouteParsers() {
    return routes.stream().map(route -> new JavaNestedRouteModelParser(route, loadingContext)).collect(toList());
  }

  @Override
  public Optional<NestedChainModelParser> getNestedChainParser() {
    return nestedChain != null ? of(new JavaNestedChainModelParser(nestedChain)) : empty();
  }

  @Override
  public Optional<CompletableComponentExecutorFactory<?>> getExecutor() {
    return operationElement.getMethod()
        .map(method -> new CompletableOperationExecutorFactory<>(enclosingType.getDeclaringClass().get(), method));
  }

  @Override
  public boolean isIgnored() {
    return IntrospectionUtils.isIgnored(operationElement, loadingContext);
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
  public boolean isAutoPaging() {
    return autoPaging;
  }

  @Override
  public boolean hasStreamingConfiguration() {
    return supportsStreaming();
  }

  @Override
  public boolean hasTransactionalAction() {
    return isTransactional();
  }

  @Override
  public boolean hasReconnectionStrategy() {
    return isConnected();
  }

  @Override
  public boolean propagatesConnectivityError() {
    return isConnected();
  }

  @Override
  public Optional<ExecutionType> getExecutionType() {
    return JavaExtensionModelParserUtils.getExecutionType(operationElement);
  }

  @Override
  public Optional<MediaTypeParser> getMediaType() {
    return JavaExtensionModelParserUtils.getMediaType(operationElement, "Operation", getName());
  }

  @Override
  public Optional<SdkExceptionHandlerFactory> getExceptionHandlerFactory() {
    return JavaErrorModelParserUtils.getExceptionHandlerFactory(operationElement, "Operation", getName());
  }

  @Override
  public Optional<MinMuleVersionParser> getResolvedMinMuleVersion() {
    return of(resolveOperationMinMuleVersion(operationElement, this.operationContainer,
                                             getContainerAnnotationMinMuleVersion(extensionElement,
                                                                                  Operations.class,
                                                                                  Operations::value,
                                                                                  this.operationContainer)));
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
  public List<ErrorModelParser> getErrorModelParsers() {
    return parseOperationErrorModels(extensionModelParser, extensionElement, operationElement);
  }

  @Override
  public Optional<OutputResolverModelParser> getOutputResolverModelParser() {
    Optional<OutputResolverModelParser> outputResolverModelParser = parseOutputResolverModelParser(operationElement);
    if (outputResolverModelParser.isPresent()) {
      return outputResolverModelParser;
    } else {
      if (getInputResolverModelParsers().isEmpty()) {
        return parseOutputResolverModelParser(extensionElement, operationContainer);
      } else {
        return empty();
      }
    }
  }

  @Override
  public Optional<AttributesResolverModelParser> getAttributesResolverModelParser() {
    return parseAttributesResolverModelParser(operationElement);
  }

  @Override
  public List<InputResolverModelParser> getInputResolverModelParsers() {
    return parseInputResolversModelParser(getParameterGroupModelParsers());
  }

  @Override
  public Optional<MetadataKeyModelParser> getMetadataKeyModelParser() {
    return getKeyIdResolverModelParser(this, operationElement.getEnclosingType(), extensionElement);
  }

  @Override
  public Optional<ScopeChainInputTypeResolverModelParser> getScopeChainInputTypeResolverModelParser() {
    if (isScope()) {
      return of(new JavaScopeChainInputTypeResolverModelParser(nestedChain));
    }
    return empty();
  }

  @Override
  public Optional<RoutesChainInputTypesResolverModelParser> getRoutesChainInputTypesResolverModelParser() {
    if (isRouter()) {
      return of(new JavaRoutesChainInputTypesResolverModelParser(routes));
    }
    return empty();
  }

  private boolean hasKeyResolverAvailable() {
    return parseKeyIdResolverModelParser(enclosingType, operationElement, "operation", getName(), extensionElement.getName())
        .map(metadataKey -> metadataKey.hasKeyIdResolver()).orElse(false);
  }

  @Override
  public Set<String> getSemanticTerms() {
    Set<String> terms = new LinkedHashSet<>();
    terms.addAll(getAllTermsFromAnnotations(operationElement::isAnnotatedWith));
    addCustomTerms(operationElement, terms);

    return terms;
  }

  @Override
  public Stream<NotificationModel> getEmittedNotificationsStream(Function<String, Optional<NotificationModel>> notificationMapper) {
    List<String> notifications =
        NotificationModelParserUtils.getEmittedNotifications(operationElement, getComponentTypeName(), getName());
    if (notifications.isEmpty()) {
      notifications = NotificationModelParserUtils.getEmittedNotifications(operationContainer, getComponentTypeName(), getName());
    }

    return notifications.stream().map(notificationMapper).filter(Optional::isPresent).map(Optional::get);
  }

  private void checkOperationIsNotAnExtension() {
    if (operationContainer.isAssignableFrom(extensionElement) || extensionElement.isAssignableFrom(operationContainer)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                operationContainer.getName(), extensionElement.getName()));
    }
  }

  private void collectAdditionalModelProperties() {
    additionalModelProperties.add(new ExtensionOperationDescriptorModelProperty(operationElement, hasDeprecatedRouterCompletion));
    operationElement.getMethod().ifPresent(method -> additionalModelProperties.add(new ImplementingMethodModelProperty(method)));
  }

  private boolean parameterOfRouterCompletionCallbackType(ExtensionParameter param) {
    return param.getType().isSameType(RouterCompletionCallback.class)
        || param.getType().isSameType(org.mule.sdk.api.runtime.process.RouterCompletionCallback.class);
  }

  @Override
  protected String getComponentTypeName() {
    if (isScope()) {
      return "Scope";
    } else if (isRouter()) {
      return "Router";
    }
    return "Operation";
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return JavaExtensionModelParserUtils.getDeprecationModel(operationElement);
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return JavaExtensionModelParserUtils.getDisplayModel(operationElement, "operation", operationElement.getName());
  }

  @Override
  public ComponentVisibility getComponentVisibility() {
    return PUBLIC;
  }

  @Override
  public Optional<StereotypeModel> getStereotype(StereotypeModelFactory factory) {
    Optional<StereotypeModel> stereotype = resolveStereotype(operationElement, "Operation", getName(), factory);

    if (!stereotype.isPresent()) {
      stereotype = resolveStereotype(operationContainer, "Operation", getName(), factory);
    }

    return stereotype;
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
