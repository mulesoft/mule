/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.extension.api.loader.parser.OutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.connection.JavaConnectionProviderModelParserUtils;
import org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils;

import java.util.List;

/**
 * Base class for model parsers which act on executable components, such as {@link OperationModel} and {@link SourceModel}.
 * <p>
 * Each implementor must focus on one specific type.
 *
 * @since 4.5.0
 */
abstract class AbstractJavaExecutableComponentModelParser extends AbstractJavaModelParser {

  protected OutputModelParser outputType;
  protected OutputModelParser outputAttributesType;
  protected boolean supportsStreaming = false;
  protected boolean connected = false;
  protected boolean transactional = false;

  /**
   * Creates a new instance
   *
   * @param extensionElement the extension's type element
   * @param loadingContext   the loading context
   */
  public AbstractJavaExecutableComponentModelParser(ExtensionElement extensionElement, ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);
  }

  /**
   * Returns a meaningful name which describes the type of component being parsed. Used for logging and error message generation
   *
   * @return the name of the parsed component type
   */
  protected abstract String getComponentTypeName();

  /**
   * @return The parsed component's name
   */
  protected abstract String getName();

  /**
   * Parses the {@code component}'s connectivity attributes and extract's the appropriate values
   *
   * @param component the connected component
   */
  protected void parseComponentConnectivity(WithParameters component) {
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
      transactional = JavaConnectionProviderModelParserUtils.isTransactional(connectionType);
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

  /**
   * Parses the {@code component}'s byte streaming attributes and extract's the appropriate values
   *
   * @param element the connected component
   */
  protected void parseComponentByteStreaming(WithAnnotations element) {
    supportsStreaming = JavaModelLoaderUtils.isInputStream(outputType.getType())
        || element.isAnnotatedWith(Streaming.class)
        || element.isAnnotatedWith(org.mule.sdk.api.annotation.Streaming.class);
  }

  private Type resolveConnectionType(ExtensionParameter connectionParameter) {
    Type connectionType = connectionParameter.getType();

    if (connectionType.getTypeName().startsWith(ConnectionProvider.class.getName())
        || connectionType.getTypeName().startsWith(org.mule.sdk.api.connectivity.ConnectionProvider.class.getName())) {
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

  public OutputModelParser getOutputType() {
    return outputType;
  }

  public OutputModelParser getAttributesOutputType() {
    return outputAttributesType;
  }

  public boolean supportsStreaming() {
    return supportsStreaming;
  }

  public boolean isConnected() {
    return connected;
  }

  public boolean requiresConnectionProvisioning() {
    return connected;
  }

  public boolean isTransactional() {
    return transactional;
  }
}
