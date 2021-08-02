/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.java.api.JavaTypeLoader.JAVA;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.handleByteStreaming;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isAutoPaging;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isNonBlocking;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isRouter;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isScope;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isIgnored;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.api.loader.java.type.WithOperationContainers;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for declaring operations through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class OperationModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String OPERATION = "Operation";
  private static final AnyType ANY_TYPE = BaseTypeBuilder.create(JAVA).anyType().build();

  private final Map<MethodElement, OperationDeclarer> operationDeclarers = new HashMap<>();
  private final ScopeModelLoaderDelegate scopesDelegate;
  private final RouterModelLoaderDelegate routersDelegate;

  OperationModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
    scopesDelegate = new ScopeModelLoaderDelegate(delegate);
    routersDelegate = new RouterModelLoaderDelegate(delegate);
  }

  void declareOperations(ExtensionDeclarer extensionDeclarer,
                         HasOperationDeclarer declarer,
                         List<OperationModelParser> operations,
                         ExtensionLoadingContext loaderContext) {

    operations.stream()
        .filter(operation -> !operation.isIgnored())
        .forEach(parser -> {
          if (parser.isScope()) {

        });
    for (OperationElement operationMethod : operations) {

      if (isIgnored(operationMethod, loaderContext)) {
        continue;
      }


      final Optional<ExtensionParameter> configParameter = loader.getConfigParameter(operationMethod);
      final Optional<ExtensionParameter> connectionParameter = loader.getConnectionParameter(operationMethod);

      if (isScope(operationMethod)) {
        scopesDelegate.declareScope(extensionDeclarer, ownerDeclarer, enclosingType, operationMethod, configParameter,
                                    connectionParameter);
        continue;
      } else if (isRouter(operationMethod)) {
        routersDelegate.declareRouter(extensionDeclarer, ownerDeclarer, enclosingType, operationMethod,
                                      configParameter,
                                      connectionParameter);
        continue;
      }

      checkDefinition(!loader.isInvalidConfigSupport(supportsConfig, configParameter, connectionParameter),
                      format("Operation '%s' is defined at the extension level but it requires a config. "
                          + "Remove such parameter or move the operation to the proper config", operationMethod.getName()));

      HasOperationDeclarer actualDeclarer = selectDeclarer(extensionDeclarer, (Declarer) ownerDeclarer,
                                                           operationMethod, configParameter, connectionParameter);

      if (operationDeclarers.containsKey(operationMethod)) {
        actualDeclarer.withOperation(operationDeclarers.get(operationMethod));
        continue;
      }

      final OperationDeclarer operationDeclarer = actualDeclarer.withOperation(operationMethod.getAlias());
      operationDeclarer.withModelProperty(new ExtensionOperationDescriptorModelProperty(operationMethod));

      Optional<Method> method = operationMethod.getMethod();
      Optional<Class<?>> declaringClass = enclosingType.getDeclaringClass();

      if (method.isPresent() && declaringClass.isPresent()) {
        operationDeclarer
            .withModelProperty(new ImplementingMethodModelProperty(method.get()))
            .withModelProperty(new CompletableComponentExecutorModelProperty(new CompletableOperationExecutorFactory(declaringClass
                .get(),
                                                                                                                     method
                                                                                                                         .get())));
      }

      loader.addExceptionEnricher(operationMethod, operationDeclarer);

      final List<ExtensionParameter> fieldParameters = methodOwner.getParameters();
      processComponentConnectivity(operationDeclarer, operationMethod, operationMethod);

      if (isNonBlocking(operationMethod)) {
        processNonBlockingOperation(operationDeclarer, operationMethod, true);
      } else {
        processBlockingOperation(supportsConfig, operationMethod, operationDeclarer);
      }

      addExecutionType(operationDeclarer, operationMethod);

      ParameterDeclarationContext declarationContext = new ParameterDeclarationContext(OPERATION,
                                                                                       operationDeclarer.getDeclaration());
      processMimeType(operationDeclarer, operationMethod);
      declareParameters(operationDeclarer, operationMethod.getParameters(), fieldParameters, declarationContext);
      operationDeclarers.put(operationMethod, operationDeclarer);
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

  private void addExecutionType(OperationDeclarer operationDeclarer, MethodElement operationMethod) {
    operationMethod.getAnnotation(Execution.class).ifPresent(a -> operationDeclarer.withExecutionType(a.value()));
  }


  }

  private void addPagedOperationModelProperty(MethodElement operationMethod, OperationDeclarer operation,
                                              boolean supportsConfig) {
    checkDefinition(supportsConfig, format("Paged operation '%s' is defined at the extension level but it requires a config, "
        + "since connections are required for paging", operationMethod.getName()));

    operation.withModelProperty(new PagedOperationModelProperty());
    operation.requiresConnection(true);
  }

  private void processPagingTx(OperationDeclarer operation, MethodElement method) {
    checkArgument(method != null, "Can't introspect a null method");
    Type returnTypeElement = method.getReturnType();
    List<TypeGeneric> generics = returnTypeElement.getGenerics();

    if (!generics.isEmpty()) {
      operation.transactional(generics.get(0).getConcreteType().isAssignableTo(TransactionalConnection.class));
    } else {
      operation.transactional(false);
    }
  }

  static void checkDefinition(boolean condition, String message) {
    if (!condition) {
      throw new IllegalOperationModelDefinitionException(message);
    }
  }

}
