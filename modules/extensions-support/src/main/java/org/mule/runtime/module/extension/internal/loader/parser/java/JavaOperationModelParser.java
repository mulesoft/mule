/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.util.NameUtils.OPERATION;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConfigParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConnectionParameter;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getCompletionCallbackParameters;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isAutoPaging;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isInputStream;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParserDecorator;
import org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils;
import org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class JavaOperationModelParser extends AbstractExecutableComponentModelParser implements OperationModelParser {

  private final ExtensionElement extensionElement;
  private final OperationElement operationMethod;
  private final OperationContainerElement methodOwnerClass;
  private final OperationContainerElement methodOwner;
  private final OperationContainerElement enclosingType;
  private final ExtensionLoadingContext loadingContext;
  private final ClassTypeLoader typeLoader;

  private final Optional<ExtensionParameter> configParameter;
  private final Optional<ExtensionParameter> connectionParameter;

  private boolean scope = false;
  private boolean autoPaging = false;

  public JavaOperationModelParser(ExtensionElement extensionElement,
                                  OperationContainerElement methodOwnerClass,
                                  OperationElement operationMethod,
                                  ClassTypeLoader typeLoader,
                                  ExtensionLoadingContext loadingContext,
                                  boolean supportsConfig) {
    super(supportsConfig);
    this.extensionElement = extensionElement;
    this.operationMethod = operationMethod;
    this.methodOwnerClass = methodOwnerClass;
    this.typeLoader = typeLoader;
    this.loadingContext = loadingContext;

    methodOwner = operationMethod.getEnclosingType();
    enclosingType = methodOwnerClass != null ? methodOwnerClass : methodOwner;
    checkOperationIsNotAnExtension();

    configParameter = getConfigParameter(operationMethod);
    connectionParameter = getConnectionParameter(operationMethod);

    parseStructure();
    collectAdditionalModelProperties();
  }

  private void parseStructure() {
    final List<ExtensionParameter> callbackParameters = getCompletionCallbackParameters(operationMethod);
    blocking = callbackParameters.isEmpty();
    connected = connectionParameter.isPresent();
    scope = operationMethod.getParameters().stream().anyMatch(ModelLoaderUtils::isProcessorChain);

    if (scope) {
      parseScope();
    }

    if (blocking) {
      parseBlockingOperation();
    } else {
      parseNonBlockingOperation(callbackParameters);
    }
  }

  private void parseScope() {
    if (blocking) {
      throw new IllegalOperationModelDefinitionException(format("Scope '%s' does not declare a '%s' parameter. One is required " +
              "for all operations that receive and execute a Chain of other components",
          getName(),
          CompletionCallback.class.getSimpleName()));
    }

    List<ExtensionParameter> processorChain =
        operationMethod.getParameters().stream().filter(ModelLoaderUtils::isProcessorChain).collect(toList());

    if (processorChain.size() > 1) {
      throw new IllegalOperationModelDefinitionException(
          format("Scope '%s' declares too many parameters of type '%s', only one input of this kind is supported."
                  + "Offending parameters are: %s",
              getName(),
              Chain.class.getSimpleName(),
              processorChain.stream().map(ExtensionParameter::getName).collect(toList())));
    }
  }

  private void parseBlockingOperation() {
    //TODO: Should be possible to parse dynamic types right here
    outputType = new DefaultOutputModelParser(operationMethod.getReturnMetadataType(), false);
    outputAttributesType = new DefaultOutputModelParser(operationMethod.getAttributesMetadataType(), false);

    processComponentConnectivity(operationMethod);

    if (autoPaging = isAutoPaging(operationMethod)) {
      parseAutoPaging();
    } else {
      supportsStreaming = isInputStream(outputType.getType())
          || operationMethod.getAnnotation(Streaming.class).isPresent()
          || operationMethod.getAnnotation(org.mule.sdk.api.annotation.Streaming.class).isPresent();
    }
  }

  private void parseAutoPaging() {
    if (!configParameter.isPresent()) {
      throw new IllegalOperationModelDefinitionException(
          format("Paged operation '%s' is defined at the extension level but it requires a config, "
              + "since connections are required for paging", operationMethod.getName()));
    }

    supportsStreaming = true;
    connected = true;
    parsePagingTx();
  }

  private void parsePagingTx() {
    Type returnTypeElement = operationMethod.getReturnType();
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

    if (isVoid(operationMethod)) {
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
      genericTypes.add(PRIMITIVE_TYPES.get("ANY"));
      genericTypes.add(PRIMITIVE_TYPES.get("ANY"));
    }

    // TODO: SHould be possible to parse dynamic types right here?
    outputType = new DefaultOutputModelParser(genericTypes.get(0), false);
    outputAttributesType = new DefaultOutputModelParser(genericTypes.get(1), false);
  }

  @Override
  public String getName() {
    return operationMethod.getAlias();
  }

  @Override
  public String getDescription() {
    return operationMethod.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    List<ParameterGroupModelParser> parameterGroupModelParsers =
        JavaExtensionModelParserUtils.getParameterGroupParsers(operationMethod.getParameters(), typeLoader);

    parameterGroupModelParsers.addAll(
        JavaExtensionModelParserUtils.getParameterGroupParsers(methodOwner.getParameters(), typeLoader,
            p -> new ParameterModelParserDecorator(p) {

              @Override
              public ExpressionSupport getExpressionSupport() {
                return NOT_SUPPORTED;
              }

              @Override
              public List<ModelProperty> getAdditionalModelProperties() {
                List<ModelProperty> modelProperties = super.getAdditionalModelProperties();
                modelProperties.add(new FieldOperationParameterModelProperty());

                return modelProperties;
              }
            }));

    return parameterGroupModelParsers;
  }

  @Override
  public boolean isIgnored() {
    return IntrospectionUtils.isIgnored(operationMethod, loadingContext);
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
  public boolean isConnected() {
    return connected;
  }

  @Override
  public boolean isTransactional() {
    return transactional;
  }

  @Override
  public boolean isNonBlocking() {
    return ModelLoaderUtils.isNonBlocking(operationMethod);
  }

  @Override
  public Optional<MediaTypeModelProperty> getMediaTypeModelProperty() {
    return operationMethod.getAnnotation(MediaType.class)
        .map(a -> new MediaTypeModelProperty(a.value(), a.strict()));
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
    additionalModelProperties.add(new ExtensionOperationDescriptorModelProperty(operationMethod));

    Optional<Method> method = operationMethod.getMethod();
    Optional<Class<?>> declaringClass = enclosingType.getDeclaringClass();

    if (method.isPresent() && declaringClass.isPresent()) {
      additionalModelProperties.add(new ImplementingMethodModelProperty(method.get()));
      additionalModelProperties.add(new CompletableComponentExecutorModelProperty(
          new CompletableOperationExecutorFactory(declaringClass.get(), method.get())));
    }

    if (autoPaging) {
      additionalModelProperties.add(new PagedOperationModelProperty());
    }
  }

  @Override
  protected String getComponentTypeName() {
    return OPERATION;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof JavaOperationModelParser) {
      return operationMethod.equals(((JavaOperationModelParser) o).operationMethod);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(operationMethod);
  }
}
