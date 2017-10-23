/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isInputStream;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceReturnType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isLifecycle;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.internal.loader.java.type.WithMessageSources;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSourceFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class for declaring sources through a {@link DefaultJavaModelLoaderDelegate}
 * @since 4.0
 */
final class SourceModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String SOURCE = "Source";

  private final Map<Class<?>, SourceDeclarer> sourceDeclarers = new HashMap<>();

  SourceModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareMessageSources(ExtensionDeclarer extensionDeclarer, HasSourceDeclarer declarer,
                             WithMessageSources typeComponent) {
    // TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
    typeComponent.getSources().forEach(source -> declareMessageSource(extensionDeclarer, declarer, source, true));
  }

  void declareMessageSource(ExtensionDeclarer extensionDeclarer,
                            HasSourceDeclarer declarer,
                            SourceElement sourceType,
                            boolean supportsConfig) {
    // TODO: MULE-9220 - Add a syntax validator which checks that the sourceType doesn't implement

    if (isLifecycle(sourceType.getDeclaringClass())) {
      throw new IllegalSourceModelDefinitionException(
                                                      format("Source class '%s' implements a lifecycle interface. Sources are not allowed to",
                                                             sourceType.getDeclaringClass().getName()));
    }

    final Optional<ExtensionParameter> configParameter = loader.getConfigParameter(sourceType);
    final Optional<ExtensionParameter> connectionParameter = loader.getConnectionParameter(sourceType);

    if (loader.isInvalidConfigSupport(supportsConfig, configParameter, connectionParameter)) {
      throw new IllegalSourceModelDefinitionException(
                                                      format("Source '%s' is defined at the extension level but it requires a config parameter. "
                                                          + "Remove such parameter or move the source to the proper config",
                                                             sourceType.getName()));
    }

    HasSourceDeclarer actualDeclarer =
        (HasSourceDeclarer) loader.selectDeclarerBasedOnConfig(extensionDeclarer, (Declarer) declarer, configParameter,
                                                               connectionParameter);

    SourceDeclarer source = sourceDeclarers.get(sourceType.getDeclaringClass());
    if (source != null) {
      actualDeclarer.withMessageSource(source);
      return;
    }

    source = actualDeclarer.withMessageSource(sourceType.getAlias());
    List<Type> sourceGenerics = sourceType.getSuperClassGenerics();

    if (sourceGenerics.size() != 2) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalModelDefinitionException(format("Message source class '%s' was expected to have 2 generic types "
          + "(one for the Payload type and another for the Attributes type) but %d were found",
                                                       sourceType.getName(),
                                                       sourceGenerics.size()));
    }

    source
        .hasResponse(sourceType.isAnnotatedWith(EmitsResponse.class))
        .requiresConnection(connectionParameter.isPresent())
        .withModelProperty(new SourceFactoryModelProperty(new DefaultSourceFactory(sourceType.getDeclaringClass())))
        .withModelProperty(new ImplementingTypeModelProperty(sourceType.getDeclaringClass()));

    processMimeType(source, sourceType);
    processComponentConnectivity(source, sourceType, sourceType);

    resolveOutputTypes(source, sourceGenerics, sourceType);

    loader.addExceptionEnricher(sourceType, source);

    declareSourceParameters(sourceType, source);
    declareSourceCallback(sourceType, source);
    sourceDeclarers.put(sourceType.getDeclaringClass(), source);
  }

  private void resolveOutputTypes(SourceDeclarer source, List<Type> sourceGenerics, SourceElement sourceType) {
    final MetadataType outputTtype = getSourceReturnType(sourceGenerics.get(0), getTypeLoader());
    source.withOutput().ofType(outputTtype);
    source.withOutputAttributes().ofType(getTypeLoader().load(sourceGenerics.get(1)));
    source.supportsStreaming(isInputStream(outputTtype) || sourceType.getAnnotation(Streaming.class).isPresent());
  }

  /**
   * Declares the parameters needed to generate messages
   */
  private void declareSourceParameters(SourceElement sourceType, SourceDeclarer source) {
    ParameterModelsLoaderDelegate parametersLoader = loader.getFieldParametersLoader();
    ParameterDeclarationContext declarationContext = new ParameterDeclarationContext(SOURCE, source.getDeclaration());
    List<ParameterDeclarer> parameters = parametersLoader.declare(source, sourceType.getParameters(), declarationContext);
    parameters.forEach(p -> p.withExpressionSupport(NOT_SUPPORTED));
  }

  private void declareSourceCallback(SourceElement sourceType, SourceDeclarer source) {
    final Optional<MethodElement> onResponseMethod = sourceType.getOnResponseMethod();
    final Optional<MethodElement> onErrorMethod = sourceType.getOnErrorMethod();
    final Optional<MethodElement> onTerminateMethod = sourceType.getOnTerminateMethod();

    // TODO: MULE-9220 add syntax validator to check that none of these use @UseConfig or @Connection
    declareSourceCallbackParameters(source, onResponseMethod, source::onSuccess);
    declareSourceCallbackParameters(source, onErrorMethod, source::onError);
    declareSourceCallbackParameters(source, onTerminateMethod, source::onTerminate);

    source.withModelProperty(new SourceCallbackModelProperty(getMethod(onResponseMethod),
                                                             getMethod(onErrorMethod),
                                                             getMethod(onTerminateMethod)));
  }


  private void declareSourceCallbackParameters(SourceDeclarer source, Optional<MethodElement> sourceCallback,
                                               Supplier<ParameterizedDeclarer> callback) {
    sourceCallback.ifPresent(method -> {
      ParameterDeclarationContext declarationContext = new ParameterDeclarationContext(SOURCE, source.getDeclaration());
      loader.getMethodParametersLoader().declare(callback.get(), method.getParameters(), declarationContext);
    });
  }

  private Optional<Method> getMethod(Optional<MethodElement> method) {
    return method.map(MethodElement::getMethod);
  }
}
