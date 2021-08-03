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
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isIgnored;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithMessageSources;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SdkSourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSdkSourceFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class for declaring sources through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class SourceModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String SOURCE = "Source";

  private final Map<SourceModelParser, SourceDeclarer> sourceDeclarers = new HashMap<>();

  SourceModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  // TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
  void declareMessageSource(ExtensionDeclarer extensionDeclarer,
                            HasSourceDeclarer ownerDeclarer,
                            List<SourceModelParser> parsers,
                            ExtensionLoadingContext context) {

    for (SourceModelParser parser : parsers) {

      if (parser.isIgnored()) {
        continue;
      }

      final boolean requiresConfig = parser.hasConfig() || parser.isConnected();
      HasSourceDeclarer actualDeclarer = requiresConfig
          ? ownerDeclarer
          : extensionDeclarer;

      if (actualDeclarer == extensionDeclarer && requiresConfig) {
        throw new IllegalSourceModelDefinitionException(
            format("Source '%s' is defined at the extension level but it requires a config parameter. "
                + "Remove such parameter or move the source to the proper config", parser.getName()));
      }


      SourceDeclarer existingDeclarer = sourceDeclarers.get(parser);
      if (existingDeclarer != null) {
        actualDeclarer.withMessageSource(existingDeclarer);
        return;
      }

      SourceDeclarer sourceDeclarer = actualDeclarer.withMessageSource(parser.getName())
          .describedAs(parser.getDescription())
          .hasResponse(parser.emitsResponse())
          .requiresConnection(parser.isConnected())
          .transactional(parser.isTransactional())
          .supportsStreaming(parser.supportsStreaming())
          .runsOnPrimaryNodeOnly(parser.runsOnPrimaryNodeOnly())
          .withModelProperty(parser.getSourceFactoryModelProperty());

      parser.getOutputType().applyOn(sourceDeclarer.withOutput());
      parser.getAttributesOutputType().applyOn(sourceDeclarer.withOutputAttributes());

      loader.getParameterModelsLoaderDelegate().declare(sourceDeclarer, parser.getParameterGroupModelParsers());
      declareSourceCallback(parser, sourceDeclarer);

      parser.getMediaTypeModelProperty().ifPresent(sourceDeclarer::withModelProperty);
      parser.getExceptionHandlerModelProperty().ifPresent(sourceDeclarer::withModelProperty);
      parser.getAdditionalModelProperties().forEach(sourceDeclarer::withModelProperty);

      sourceDeclarers.put(parser, sourceDeclarer);
    }
  }

  private void declareSourceCallback(SourceElement sourceType, SourceDeclarer source) {
    final Optional<MethodElement> onResponseMethod = sourceType.getOnResponseMethod();
    final Optional<MethodElement> onErrorMethod = sourceType.getOnErrorMethod();
    final Optional<MethodElement> onTerminateMethod = sourceType.getOnTerminateMethod();
    final Optional<MethodElement> onBackPressureMethod = sourceType.getOnBackPressureMethod();

    // TODO: MULE-9220 add syntax validator to check that none of these use @UseConfig or @Connection
    declareSourceCallbackParameters(source, onResponseMethod, source::onSuccess);
    declareSourceCallbackParameters(source, onErrorMethod, source::onError);
    declareSourceCallbackParameters(source, onTerminateMethod, source::onTerminate);
    declareSourceCallbackParameters(source, onBackPressureMethod, source::onBackPressure);

    source.withModelProperty(new SourceCallbackModelProperty(getMethod(onResponseMethod),
                                                             getMethod(onErrorMethod),
                                                             getMethod(onTerminateMethod),
                                                             getMethod(onBackPressureMethod)));
  }


  private void declareSourceCallbackParameters(SourceDeclarer source, Optional<MethodElement> sourceCallback,
                                               Supplier<ParameterizedDeclarer> callback) {
    sourceCallback.ifPresent(method -> {
      ParameterDeclarationContext declarationContext = new ParameterDeclarationContext(SOURCE, source.getDeclaration());
      loader.getMethodParametersLoader().declare(callback.get(), method.getParameters(), declarationContext);
    });
  }

  private Optional<Method> getMethod(Optional<MethodElement> method) {
    return method.flatMap(MethodElement::getMethod);
  }
}
