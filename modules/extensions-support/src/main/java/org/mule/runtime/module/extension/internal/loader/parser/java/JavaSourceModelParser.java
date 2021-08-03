/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
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
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getFieldParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.isInputStream;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getRoutes;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;

import org.mule.metadata.api.ClassTypeLoader;
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
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedChainModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedRouteModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class JavaSourceModelParser extends AbstractExecutableComponentModelParser implements SourceModelParser {

  private final ExtensionElement extensionElement;
  private final SourceElement sourceElement;
  private final OperationContainerElement methodOwnerClass;
  private final OperationContainerElement methodOwner;
  private final OperationContainerElement enclosingType;
  private final ExtensionLoadingContext loadingContext;
  private final ClassTypeLoader typeLoader;

  private final Optional<ExtensionParameter> configParameter;
  private final Optional<ExtensionParameter> connectionParameter;


  public JavaSourceModelParser(ExtensionElement extensionElement,
                               OperationContainerElement methodOwnerClass,
                               SourceElement sourceElement,
                               ClassTypeLoader typeLoader,
                               ExtensionLoadingContext loadingContext,
                               boolean supportsConfig) {
    super(supportsConfig);
    this.extensionElement = extensionElement;
    this.sourceElement = sourceElement;
    this.methodOwnerClass = methodOwnerClass;
    this.typeLoader = typeLoader;
    this.loadingContext = loadingContext;

    methodOwner = sourceElement.getEnclosingType();
    enclosingType = methodOwnerClass != null ? methodOwnerClass : methodOwner;
    checkOperationIsNotAnExtension();

    configParameter = getConfigParameter(sourceElement);
    connectionParameter = getConnectionParameter(sourceElement);

    parseStructure();
    collectAdditionalModelProperties();
  }

  private void parseStructure() {
    final List<ExtensionParameter> callbackParameters = getCompletionCallbackParameters(sourceElement);
    blocking = callbackParameters.isEmpty();
    connected = connectionParameter.isPresent();

    if (blocking) {
      parseBlockingOperation();
    } else {
      parseNonBlockingOperation(callbackParameters);
    }
  }


  private void parseBlockingOperation() {
    //TODO: Should be possible to parse dynamic types right here
    outputType = new DefaultOutputModelParser(sourceElement.getReturnMetadataType(), false);
    outputAttributesType = new DefaultOutputModelParser(sourceElement.getAttributesMetadataType(), false);

    processComponentConnectivity(sourceElement);

    if (autoPaging = JavaExtensionModelParserUtils.isAutoPaging(sourceElement)) {
      parseAutoPaging();
    } else {
      supportsStreaming = isInputStream(outputType.getType())
          || sourceElement.getAnnotation(Streaming.class).isPresent()
          || sourceElement.getAnnotation(org.mule.sdk.api.annotation.Streaming.class).isPresent();
    }
  }

  private void parseAutoPaging() {
    if (!configParameter.isPresent()) {
      throw new IllegalOperationModelDefinitionException(
          format("Paged operation '%s' is defined at the extension level but it requires a config, "
              + "since connections are required for paging", sourceElement.getName()));
    }

    supportsStreaming = true;
    connected = true;
    additionalModelProperties.add(new PagedOperationModelProperty());
    parsePagingTx();
  }

  private void parsePagingTx() {
    Type returnTypeElement = sourceElement.getReturnType();
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

    if (isVoid(sourceElement)) {
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
    return sourceElement.getAlias();
  }

  @Override
  public String getDescription() {
    return sourceElement.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    List<ExtensionParameter> methodParameters;

    if (isRouter()) {
      methodParameters = sourceElement.getParameters().stream()
          .filter(p -> !isRoute(p) && !isRouterCallback(p))
          .collect(toList());
    } else {
      methodParameters = sourceElement.getParameters();
    }

    List<ParameterGroupModelParser> parameterGroupModelParsers = getParameterGroupParsers(methodParameters, typeLoader);
    parameterGroupModelParsers.addAll(getFieldParameterGroupParsers(methodOwner.getParameters(), typeLoader));

    return parameterGroupModelParsers;
  }

  @Override
  public List<NestedRouteModelParser> getNestedRouteParsers() {
    return routes.stream().map(r -> new JavaNestedRouteModelParser(r, typeLoader)).collect(toList());
  }

  @Override
  public Optional<NestedChainModelParser> getNestedChainParser() {
    return nestedChain != null ? of(new JavaNestedChainModelParser(nestedChain)) : empty();
  }

  @Override
  public CompletableComponentExecutorModelProperty getExecutorModelProperty() {
    return new CompletableComponentExecutorModelProperty(
        new CompletableOperationExecutorFactory(enclosingType.getDeclaringClass().get(), sourceElement.getMethod().get()));
  }

  @Override
  public boolean isIgnored() {
    return IntrospectionUtils.isIgnored(sourceElement, loadingContext);
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
    return sourceElement.getAnnotation(Execution.class).map(Execution::value);
  }

  @Override
  public boolean isNonBlocking() {
    return !blocking;
  }

  @Override
  public Optional<MediaTypeModelProperty> getMediaTypeModelProperty() {
    return sourceElement.getAnnotation(MediaType.class)
        .map(a -> new MediaTypeModelProperty(a.value(), a.strict()));
  }

  @Override
  public Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty() {
    return getExceptionEnricherFactory(sourceElement).map(ExceptionHandlerModelProperty::new);
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
    if (methodOwner.isAssignableFrom(extensionElement) || extensionElement.isAssignableFrom(methodOwner)) {
      throw new IllegalOperationModelDefinitionException(
          format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
              methodOwner.getName(), extensionElement.getName()));
    }
  }

  private void collectAdditionalModelProperties() {
    additionalModelProperties.add(new ExtensionOperationDescriptorModelProperty(sourceElement));

    Optional<Method> method = sourceElement.getMethod();
    Optional<Class<?>> declaringClass = enclosingType.getDeclaringClass();

    if (method.isPresent() && declaringClass.isPresent()) {
      additionalModelProperties.add(new ImplementingMethodModelProperty(method.get()));
    }
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
    if (o instanceof JavaSourceModelParser) {
      return sourceElement.equals(((JavaSourceModelParser) o).sourceElement);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(sourceElement);
  }
}
