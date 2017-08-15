/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isInputStream;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getGenerics;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnAttributesType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.springframework.core.ResolvableType.forMethodReturnType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.ExtensionOf;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExtendingOperationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.WithOperationContainers;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveOperationExecutorFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ResolvableType;

/**
 * Helper class for declaring operations through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class OperationModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String OPERATION = "Operation";

  private final Map<MethodElement, OperationDeclarer> operationDeclarers = new HashMap<>();

  OperationModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareOperations(ExtensionDeclarer extensionDeclarer, HasOperationDeclarer declarer,
                         WithOperationContainers operationContainers) {
    operationContainers.getOperationContainers()
        .forEach(operationContainer -> declareOperations(extensionDeclarer, declarer, operationContainer));
  }

  void declareOperations(ExtensionDeclarer extensionDeclarer, HasOperationDeclarer declarer,
                         OperationContainerElement operationsContainer) {
    declareOperations(extensionDeclarer, declarer, operationsContainer.getDeclaringClass(), operationsContainer.getOperations(),
                      true);
  }

  void declareOperations(ExtensionDeclarer extensionDeclarer,
                         HasOperationDeclarer declarer,
                         final Class<?> methodOwnerClass,
                         List<MethodElement> operations,
                         boolean supportsConfig) {

    for (MethodElement operationMethod : operations) {
      Class<?> declaringClass = methodOwnerClass != null ? methodOwnerClass : operationMethod.getDeclaringClass();
      checkOperationIsNotAnExtension(declaringClass);

      final Method method = operationMethod.getMethod();
      final Optional<ExtensionParameter> configParameter = loader.getConfigParameter(operationMethod);
      final Optional<ExtensionParameter> connectionParameter = loader.getConnectionParameter(operationMethod);

      if (loader.isInvalidConfigSupport(supportsConfig, configParameter, connectionParameter)) {
        throw new IllegalOperationModelDefinitionException(format(
                                                                  "Operation '%s' is defined at the extension level but it requires a config. "
                                                                      + "Remove such parameter or move the operation to the proper config",
                                                                  method.getName()));
      }

      HasOperationDeclarer actualDeclarer =
          selectDeclarer(extensionDeclarer, (Declarer) declarer, operationMethod, configParameter, connectionParameter);

      if (operationDeclarers.containsKey(operationMethod)) {
        actualDeclarer.withOperation(operationDeclarers.get(operationMethod));
        continue;
      }

      final OperationDeclarer operation = actualDeclarer.withOperation(operationMethod.getAlias())
          .withModelProperty(new ImplementingMethodModelProperty(method))
          .withModelProperty(new OperationExecutorModelProperty(new ReflectiveOperationExecutorFactory<>(declaringClass,
                                                                                                         method)));

      loader.addExceptionEnricher(operationMethod, operation);
      processComponentConnectivity(operation, operationMethod, operationMethod);

      if (!processNonBlockingOperation(operation, operationMethod)) {

        operation.blocking(true);
        operation.withOutputAttributes().ofType(getMethodReturnAttributesType(method, loader.getTypeLoader()));

        if (isAutoPaging(operationMethod)) {
          operation.supportsStreaming(true).withOutput().ofType(getMethodReturnType(method, loader.getTypeLoader()));
          addPagedOperationModelProperty(operationMethod, operation, supportsConfig);
          processPagingTx(operation, method);
        } else {
          final MetadataType outputType = getMethodReturnType(method, loader.getTypeLoader());
          operation.withOutput().ofType(outputType);

          handleByteStreaming(method, operation, outputType);
        }
      }

      addExecutionType(operation, operationMethod);
      ParameterDeclarationContext declarationContext = new ParameterDeclarationContext(OPERATION, operation.getDeclaration());
      loader.getMethodParametersLoader().declare(operation, operationMethod.getParameters(), declarationContext);
      calculateExtendedTypes(declaringClass, method, operation);
      operationDeclarers.put(operationMethod, operation);
    }
  }

  private HasOperationDeclarer selectDeclarer(ExtensionDeclarer extensionDeclarer, Declarer declarer,
                                              MethodElement operationMethod, Optional<ExtensionParameter> configParameter,
                                              Optional<ExtensionParameter> connectionParameter) {
    if (isAutoPaging(operationMethod)) {
      return (HasOperationDeclarer) declarer;
    }

    return (HasOperationDeclarer) loader.selectDeclarerBasedOnConfig(extensionDeclarer, declarer, configParameter,
                                                                     connectionParameter);
  }

  private void handleByteStreaming(Method method, OperationDeclarer operation, MetadataType outputType) {
    operation.supportsStreaming(isInputStream(outputType) || method.getAnnotation(Streaming.class) != null);
  }

  private boolean processNonBlockingOperation(OperationDeclarer operation, MethodElement operationMethod) {
    List<ExtensionParameter> callbackParameters = operationMethod.getParameters().stream()
        .filter(p -> CompletionCallback.class.equals(p.getType().getDeclaringClass()))
        .collect(toList());

    if (callbackParameters.isEmpty()) {
      return false;
    }

    if (callbackParameters.size() > 1) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Operation '%s' defines more than one %s parameters. Only one is allowed",
                                                                operationMethod.getAlias(),
                                                                CompletionCallback.class.getSimpleName()));
    }

    if (!isVoid(operationMethod.getMethod())) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Operation '%s' has a parameter of type %s but is not void. Non-blocking operations have to be declared as void and the "
                                                                    + "return type provided through the callback",
                                                                operationMethod.getAlias(),
                                                                CompletionCallback.class.getSimpleName()));
    }

    ExtensionParameter callbackParameter = callbackParameters.get(0);
    java.lang.reflect.Parameter methodParameter = (java.lang.reflect.Parameter) callbackParameter.getDeclaringElement();
    List<MetadataType> genericTypes = getGenerics(methodParameter.getParameterizedType(), loader.getTypeLoader());

    if (genericTypes.isEmpty()) {
      throw new IllegalParameterModelDefinitionException(format("Generics are mandatory on the %s parameter of Operation '%s'",
                                                                CompletionCallback.class.getSimpleName(),
                                                                operationMethod.getAlias()));
    }

    operation.withOutput().ofType(genericTypes.get(0));
    operation.withOutputAttributes().ofType(genericTypes.get(1));
    operation.blocking(false);
    handleByteStreaming(operationMethod.getMethod(), operation, genericTypes.get(0));

    return true;
  }

  private void addExecutionType(OperationDeclarer operationDeclarer, MethodElement operationMethod) {
    operationMethod.getAnnotation(Execution.class).ifPresent(a -> operationDeclarer.withExecutionType(a.value()));
  }

  private void checkOperationIsNotAnExtension(Class<?> operationType) {
    if (operationType.isAssignableFrom(getExtensionType()) || getExtensionType().isAssignableFrom(operationType)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                operationType.getName(), getExtensionType().getName()));
    }
  }

  private void calculateExtendedTypes(Class<?> actingClass, Method method, OperationDeclarer operation) {
    ExtensionOf extensionOf = method.getAnnotation(ExtensionOf.class);
    if (extensionOf == null) {
      extensionOf = actingClass.getAnnotation(ExtensionOf.class);
    }

    if (extensionOf != null) {
      operation.withModelProperty(new ExtendingOperationModelProperty(extensionOf.value()));
    } else if (isExtensible()) {
      operation.withModelProperty(new ExtendingOperationModelProperty(getExtensionType()));
    }
  }

  private void addPagedOperationModelProperty(MethodElement operationMethod, OperationDeclarer operation,
                                              boolean supportsConfig) {
    if (!supportsConfig) {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "Paged operation '%s' is defined at the extension level but it requires a config, since connections "
                                                                    + "are required for paging",
                                                                operationMethod.getName()));
    }

    operation.withModelProperty(new PagedOperationModelProperty());
    operation.requiresConnection(true);
  }

  private void processPagingTx(OperationDeclarer operation, Method method) {
    checkArgument(method != null, "Can't introspect a null method");
    ResolvableType connectionType = forMethodReturnType(method).getGeneric(0);
    operation.transactional(TransactionalConnection.class.isAssignableFrom(connectionType.getRawClass()));
  }

  private boolean isAutoPaging(MethodElement operationMethod) {
    return PagingProvider.class.isAssignableFrom(operationMethod.getReturnType());
  }

  private boolean isExtensible() {
    return getExtensionType().getAnnotation(Extensible.class) != null;
  }

}
