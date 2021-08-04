/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;

import java.util.LinkedList;
import java.util.List;

abstract class AbstractExecutableComponentModelParser {

  protected OutputModelParser outputType;
  protected OutputModelParser outputAttributesType;
  protected boolean supportsStreaming = false;
  protected boolean connected = false;
  protected boolean transactional = false;
  protected final List<ModelProperty> additionalModelProperties = new LinkedList<>();

  protected void processComponentConnectivity(WithParameters component) {
    List<ExtensionParameter> connectionParameters = component.getParametersAnnotatedWith(Connection.class);
    if (connectionParameters.isEmpty()) {
      connectionParameters = component.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.Connection.class);
    }

    if (connectionParameters.isEmpty()) {
      connected = false;
      transactional = false;
    } else if (connectionParameters.size() == 1) {
      connected = true;
      ExtensionParameter connectionParameter = connectionParameters.iterator().next();
      final Type connectionType = resolveConnectionType(connectionParameter);
      transactional = connectionType.isAssignableTo(TransactionalConnection.class);
      additionalModelProperties.add(new ConnectivityModelProperty(connectionType));
    } else {
      throw new IllegalOperationModelDefinitionException(format(
          "%s '%s' defines %d parameters annotated with @%s. Only one is allowed",
          getComponentTypeName(),
          getName(),
          connectionParameters.size(),
          Connection.class.getSimpleName()));
    }
  }

  private Type resolveConnectionType(ExtensionParameter connectionParameter) {
    Type connectionType = connectionParameter.getType();

    if (connectionType.getTypeName().startsWith(ConnectionProvider.class.getName())) {
      List<TypeGeneric> generics = connectionType.getGenerics();
      if (generics.size() == 0) {
        throw new IllegalOperationModelDefinitionException(format(
            "%s '%s' defines a %s without a connection type. Please add the generic",
            getComponentTypeName(),
            getName(),
            ConnectionProvider.class.getSimpleName()));
      }
      return generics.get(0).getConcreteType();
    }
    return connectionType;
  }

  protected abstract String getComponentTypeName();

  protected abstract String getName();
}
