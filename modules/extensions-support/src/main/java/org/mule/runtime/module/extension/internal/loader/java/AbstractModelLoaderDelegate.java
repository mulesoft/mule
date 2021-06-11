/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.api.loader.java.type.WithAlias;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.util.List;
import java.util.Optional;

/**
 * Base class for sub delegates of {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
abstract class AbstractModelLoaderDelegate {

  protected final DefaultJavaModelLoaderDelegate loader;

  AbstractModelLoaderDelegate(DefaultJavaModelLoaderDelegate loader) {
    this.loader = loader;
  }

  protected ClassLoader getExtensionClassLoader() {
    return getExtensionType()
        .map(Class::getClassLoader)
        .orElseGet(ExtensionModel.class::getClassLoader);
  }

  protected Optional<Class<?>> getExtensionType() {
    return Optional.ofNullable(loader.getExtensionType());
  }

  protected ExtensionElement getExtensionElement() {
    return loader.getExtensionElement();
  }

  ConfigModelLoaderDelegate getConfigLoaderDelegate() {
    return loader.getConfigLoaderDelegate();
  }

  OperationModelLoaderDelegate getOperationModelLoaderDelegate() {
    return loader.getOperationLoaderDelegate();
  }

  SourceModelLoaderDelegate getSourceModelLoaderDelegate() {
    return loader.getSourceModelLoaderDelegate();
  }

  ConnectionProviderModelLoaderDelegate getConnectionProviderModelLoaderDelegate() {
    return loader.getConnectionProviderModelLoaderDelegate();
  }

  OperationModelLoaderDelegate getOperationLoaderDelegate() {
    return loader.getOperationLoaderDelegate();
  }

  FunctionModelLoaderDelegate getFunctionModelLoaderDelegate() {
    return loader.getFunctionModelLoaderDelegate();
  }

  ClassTypeLoader getTypeLoader() {
    return loader.getTypeLoader();
  }

  void processComponentConnectivity(ExecutableComponentDeclarer componentDeclarer, WithParameters component, WithAlias alias) {
    final List<ExtensionParameter> connectionParameters = component.getParametersAnnotatedWith(Connection.class);
    connectionParameters.addAll(component.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.Connection.class));
    if (connectionParameters.isEmpty()) {
      componentDeclarer.requiresConnection(false).transactional(false);
    } else if (connectionParameters.size() == 1) {
      ExtensionParameter connectionParameter = connectionParameters.iterator().next();
      final Type connectionType = resolveConnectionType(componentDeclarer, connectionParameter, alias);

      componentDeclarer.requiresConnection(true)
          .transactional(connectionType.isAssignableTo(TransactionalConnection.class))
          .withModelProperty(new ConnectivityModelProperty(connectionType));
    } else {
      throw new IllegalOperationModelDefinitionException(format(
                                                                "%s '%s' defines %d parameters annotated with @%s. Only one is allowed",
                                                                getComponentDeclarationTypeName(componentDeclarer
                                                                    .getDeclaration()),
                                                                alias.getAlias(),
                                                                connectionParameters.size(),
                                                                Connection.class.getSimpleName()));
    }
  }

  private Type resolveConnectionType(ExecutableComponentDeclarer componentDeclarer, ExtensionParameter connectionParameter,
                                     WithAlias alias) {

    Type connectionType = connectionParameter.getType();

    if (connectionType.getTypeName().startsWith(ConnectionProvider.class.getName())) {
      List<TypeGeneric> generics = connectionType.getGenerics();
      if (generics.size() == 0) {
        throw new IllegalOperationModelDefinitionException(format(
                                                                  "%s '%s' defines a %s without a connection type. Please add the generic",
                                                                  getComponentDeclarationTypeName(componentDeclarer
                                                                      .getDeclaration()),
                                                                  alias.getAlias(),
                                                                  ConnectionProvider.class.getSimpleName()));
      }
      return generics.get(0).getConcreteType();
    }
    return connectionType;
  }

  void processMimeType(HasModelProperties declarer, WithAnnotations element) {
    element.getAnnotation(MediaType.class).ifPresent(a -> declarer.withModelProperty(
                                                                                     new MediaTypeModelProperty(a.value(),
                                                                                                                a.strict())));
  }

  void declareParameters(ComponentDeclarer component,
                         List<ExtensionParameter> methodParameters,
                         List<ExtensionParameter> fieldParameters,
                         ParameterDeclarationContext declarationContext) {

    loader.getMethodParametersLoader().declare(component, methodParameters, declarationContext);
    loader.getFieldParametersLoader().declare(component, fieldParameters, declarationContext).forEach(p -> {
      p.withExpressionSupport(NOT_SUPPORTED);
      p.withModelProperty(new FieldOperationParameterModelProperty());
    });
  }

}
