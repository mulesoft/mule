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
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.handleByteStreaming;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isAutoPaging;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isNonBlocking;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isRouter;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isScope;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getGenerics;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnAttributesType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.springframework.core.ResolvableType.forMethodReturnType;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
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
  private final ScopeModelLoaderDelegate scopesDelegate;
  private final RouterModelLoaderDelegate routersDelegate;

  OperationModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
    scopesDelegate = new ScopeModelLoaderDelegate(delegate);
    routersDelegate = new RouterModelLoaderDelegate(delegate);
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
                         HasOperationDeclarer ownerDeclarer,
                         final Class<?> methodOwnerClass,
                         List<MethodElement> operations,
                         boolean supportsConfig) {

    for (MethodElement operationMethod : operations) {
      Class<?> declaringClass = methodOwnerClass != null ? methodOwnerClass : operationMethod.getDeclaringClass();
      checkOperationIsNotAnExtension(declaringClass);

      final Method method = operationMethod.getMethod();
      final Optional<ExtensionParameter> configParameter = loader.getConfigParameter(operationMethod);
      final Optional<ExtensionParameter> connectionParameter = loader.getConnectionParameter(operationMethod);

      if (isScope(operationMethod)) {
        scopesDelegate.declareScope(extensionDeclarer, ownerDeclarer, methodOwnerClass, operationMethod, method, configParameter,
                                    connectionParameter);
        continue;
      } else if (isRouter(operationMethod)) {
        routersDelegate.declareRouter(extensionDeclarer, ownerDeclarer, methodOwnerClass, operationMethod, method,
                                      configParameter,
                                      connectionParameter);
        continue;
      }

      checkDefinition(!loader.isInvalidConfigSupport(supportsConfig, configParameter, connectionParameter),
                      format("Operation '%s' is defined at the extension level but it requires a config. "
                          + "Remove such parameter or move the operation to the proper config",
                             method.getName()));

      HasOperationDeclarer actualDeclarer = selectDeclarer(extensionDeclarer, (Declarer) ownerDeclarer,
                                                           operationMethod, configParameter, connectionParameter);

      if (operationDeclarers.containsKey(operationMethod)) {
        actualDeclarer.withOperation(operationDeclarers.get(operationMethod));
        continue;
      }

      final OperationDeclarer operationDeclarer = actualDeclarer.withOperation(operationMethod.getAlias())
          .withModelProperty(new ImplementingMethodModelProperty(method))
          .withModelProperty(new ComponentExecutorModelProperty(new ReflectiveOperationExecutorFactory<>(declaringClass,
                                                                                                         method)));

      loader.addExceptionEnricher(operationMethod, operationDeclarer);

      declareOperation(operationDeclarer, supportsConfig, operationMethod, method);

      operationDeclarers.put(operationMethod, operationDeclarer);
    }
  }

  private void declareOperation(OperationDeclarer operation, boolean supportsConfig, MethodElement operationMethod,
                                Method method) {
    processComponentConnectivity(operation, operationMethod, operationMethod);

    if (isNonBlocking(operationMethod)) {
      processNonBlockingOperation(operation, operationMethod, true, loader.getTypeLoader());
    } else {
      processBlockingOperation(supportsConfig, operationMethod, method, operation);
    }

    processMimeType(operation, operationMethod);
    addExecutionType(operation, operationMethod);
    ParameterDeclarationContext declarationContext = new ParameterDeclarationContext(OPERATION, operation.getDeclaration());
    loader.getMethodParametersLoader().declare(operation, operationMethod.getParameters(), declarationContext);
  }

  private void processBlockingOperation(boolean supportsConfig, MethodElement operationMethod, Method method,
                                        OperationDeclarer operation) {
    operation.blocking(true);
    operation.withOutputAttributes().ofType(getMethodReturnAttributesType(method, loader.getTypeLoader()));

    final MetadataType outputType = getMethodReturnType(method, loader.getTypeLoader());

    if (isAutoPaging(operationMethod)) {
      operation.supportsStreaming(true).withOutput().ofType(outputType);
      addPagedOperationModelProperty(operationMethod, operation, supportsConfig);
      processPagingTx(operation, method);
    } else {
      operation.withOutput().ofType(outputType);

      handleByteStreaming(method, operation, outputType);
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

  static void processNonBlockingOperation(OperationDeclarer operation, MethodElement operationMethod, boolean allowStreaming,
                                          ClassTypeLoader typeLoader) {
    List<ExtensionParameter> callbackParameters = operationMethod.getParameters().stream()
        .filter(p -> CompletionCallback.class.equals(p.getType().getDeclaringClass()))
        .collect(toList());

    checkDefinition(!callbackParameters.isEmpty(),
                    format("Operation '%s' does not declare a '%s' parameter. One is required for a non-blocking operation",
                           operationMethod.getAlias(),
                           CompletionCallback.class.getSimpleName()));

    checkDefinition(callbackParameters.size() <= 1,
                    format("Operation '%s' defines more than one %s parameters. Only one is allowed",
                           operationMethod.getAlias(), CompletionCallback.class.getSimpleName()));

    checkDefinition(isVoid(operationMethod.getMethod()), format("Operation '%s' has a parameter of type %s but is not void. "
        + "Non-blocking operations have to be declared as void and the "
        + "return type provided through the callback",
                                                                operationMethod.getAlias(),
                                                                CompletionCallback.class.getSimpleName()));

    ExtensionParameter callbackParameter = callbackParameters.get(0);
    java.lang.reflect.Parameter methodParameter = (java.lang.reflect.Parameter) callbackParameter.getDeclaringElement();
    List<MetadataType> genericTypes = getGenerics(methodParameter.getParameterizedType(), typeLoader);

    if (genericTypes.isEmpty()) {
      throw new IllegalParameterModelDefinitionException(format("Generics are mandatory on the %s parameter of Operation '%s'",
                                                                CompletionCallback.class.getSimpleName(),
                                                                operationMethod.getAlias()));
    }

    operation.withOutput().ofType(genericTypes.get(0));
    operation.withOutputAttributes().ofType(genericTypes.get(1));
    operation.blocking(false);

    if (allowStreaming) {
      handleByteStreaming(operationMethod.getMethod(), operation, genericTypes.get(0));
    } else {
      operation.supportsStreaming(false);
    }
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

  private void addPagedOperationModelProperty(MethodElement operationMethod, OperationDeclarer operation,
                                              boolean supportsConfig) {
    checkDefinition(supportsConfig, format("Paged operation '%s' is defined at the extension level but it requires a config, "
        + "since connections are required for paging", operationMethod.getName()));

    operation.withModelProperty(new PagedOperationModelProperty());
    operation.requiresConnection(true);
  }

  private void processPagingTx(OperationDeclarer operation, Method method) {
    checkArgument(method != null, "Can't introspect a null method");
    ResolvableType connectionType = forMethodReturnType(method).getGeneric(0);
    operation.transactional(TransactionalConnection.class.isAssignableFrom(connectionType.getRawClass()));
  }

  static void checkDefinition(boolean condition, String message) {
    if (!condition) {
      throw new IllegalOperationModelDefinitionException(message);
    }
  }

}
