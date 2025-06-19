/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static org.mule.runtime.extension.api.property.BackPressureStrategyModelProperty.getDefault;
import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.declareEmittedNotifications;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.declarerWithMmv;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.declareMetadataModelProperties;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.SourceModelParser;
import org.mule.runtime.extension.api.loader.parser.SourceModelParser.SourceCallbackModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.extension.api.loader.parser.metadata.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SdkSourceFactoryModelProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class for declaring sources through a {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
final class SourceModelLoaderDelegate extends AbstractComponentModelLoaderDelegate {

  private final Map<SourceModelParser, SourceDeclarer> sourceDeclarers = new HashMap<>();

  SourceModelLoaderDelegate(DefaultExtensionModelLoaderDelegate delegate) {
    super(delegate);
  }

  // TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
  void declareMessageSources(ExtensionDeclarer extensionDeclarer,
                             HasSourceDeclarer ownerDeclarer,
                             List<SourceModelParser> parsers,
                             ExtensionLoadingContext context) {

    for (SourceModelParser parser : parsers) {

      if (parser.isIgnored()) {
        continue;
      }

      final boolean requiresConfig = requiresConfig(parser);
      HasSourceDeclarer actualDeclarer = requiresConfig
          ? ownerDeclarer
          : extensionDeclarer;

      if (actualDeclarer == extensionDeclarer && requiresConfig) {
        throw new IllegalSourceModelDefinitionException(
                                                        format("Source '%s' is defined at the extension level but it requires a config parameter. "
                                                            + "Remove such parameter or move the source to the proper config",
                                                               parser.getName()));
      }


      SourceDeclarer existingDeclarer = sourceDeclarers.get(parser);
      if (existingDeclarer != null) {
        actualDeclarer.withMessageSource(existingDeclarer);
        continue;
      }

      SourceDeclarer sourceDeclarer = actualDeclarer.withMessageSource(parser.getName())
          .describedAs(parser.getDescription())
          .hasResponse(parser.emitsResponse())
          .requiresConnection(parser.isConnected())
          .transactional(parser.isTransactional())
          .supportsStreaming(parser.supportsStreaming())
          .withVisibility(parser.getComponentVisibility());

      parser.getDeprecationModel().ifPresent(sourceDeclarer::withDeprecation);
      parser.getDisplayModel().ifPresent(d -> sourceDeclarer.getDeclaration().setDisplayModel(d));
      parser.getSourceFactory().map(SdkSourceFactoryModelProperty::new).ifPresent(sourceDeclarer::withModelProperty);

      parser.getOutputType().applyOn(sourceDeclarer.withOutput());
      parser.getAttributesOutputType().applyOn(sourceDeclarer.withOutputAttributes());
      if (context.isResolveMinMuleVersion()) {
        parser.getResolvedMinMuleVersion().ifPresent(resolvedMMV -> declarerWithMmv(sourceDeclarer, resolvedMMV));
      }

      Optional<OutputResolverModelParser> outputResolverModelParser = parser.getOutputResolverModelParser();
      Optional<AttributesResolverModelParser> attributesResolverModelParser = parser.getAttributesResolverModelParser();
      Optional<MetadataKeyModelParser> keyIdResolverModelParser = parser.getMetadataKeyModelParser();

      loader.getParameterModelsLoaderDelegate().declare(sourceDeclarer, parser.getParameterGroupModelParsers(), context);

      parser.getMediaType()
          .map(mediaTypeParser -> new MediaTypeModelProperty(mediaTypeParser.getMimeType(), mediaTypeParser.isStrict()))
          .ifPresent(sourceDeclarer::withModelProperty);
      parser.getExceptionHandlerFactory()
          .map(ExceptionHandlerModelProperty::new)
          .ifPresent(sourceDeclarer::withModelProperty);
      loader.registerOutputTypes(sourceDeclarer.getDeclaration());

      declareMetadataModelProperties(sourceDeclarer.getDeclaration(), outputResolverModelParser,
                                     attributesResolverModelParser,
                                     emptyList(),
                                     keyIdResolverModelParser,
                                     parser.isConnected());

      addSemanticTerms(sourceDeclarer.getDeclaration(), parser);
      declareEmittedNotifications(parser, sourceDeclarer, loader::getNotificationModel);
      getStereotypeModelLoaderDelegate().addStereotypes(
                                                        parser,
                                                        sourceDeclarer,
                                                        of(() -> getStereotypeModelLoaderDelegate()
                                                            .getDefaultSourceStereotype(parser.getName())));
      parser.getAdditionalModelProperties().forEach(sourceDeclarer::withModelProperty);

      Optional<SourceCallbackModelParser> successCallbackSourceCallbackModelParser = parser.getOnSuccessCallbackParser();
      if (successCallbackSourceCallbackModelParser.isPresent()) {
        SourceCallbackDeclarer onSuccessSourceCallbackDeclarer = sourceDeclarer.onSuccess();

        List<InputResolverModelParser> sourceCallbackInputResolverModelParsers =
            successCallbackSourceCallbackModelParser.get().getInputResolverModelParsers();

        declareMetadataModelProperties(onSuccessSourceCallbackDeclarer.getDeclaration(), empty(), empty(),
                                       sourceCallbackInputResolverModelParsers, empty(), parser.isConnected());

        declareSourceCallbackParameters(successCallbackSourceCallbackModelParser, () -> onSuccessSourceCallbackDeclarer, context);
      }

      Optional<SourceCallbackModelParser> errorCallbackSourceCallbackModelParser = parser.getOnErrorCallbackParser();
      if (errorCallbackSourceCallbackModelParser.isPresent()) {
        SourceCallbackDeclarer onErrorSourceCallbackDeclarer = sourceDeclarer.onError();

        List<InputResolverModelParser> sourceCallbackInputResolverModelParsers =
            errorCallbackSourceCallbackModelParser.get().getInputResolverModelParsers();

        declareMetadataModelProperties(onErrorSourceCallbackDeclarer.getDeclaration(), empty(), empty(),
                                       sourceCallbackInputResolverModelParsers, empty(), parser.isConnected());

        declareSourceCallbackParameters(parser.getOnErrorCallbackParser(), () -> onErrorSourceCallbackDeclarer, context);
      }

      // TODO: MULE-9220 add syntax validator to check that none of these use @UseConfig or @Connection
      declareSourceCallbackParameters(parser.getOnTerminateCallbackParser(), sourceDeclarer::onTerminate, context);
      declareSourceCallbackParameters(parser.getOnBackPressureCallbackParser(), sourceDeclarer::onBackPressure, context);

      sourceDeclarer.withModelProperty(parser.getSourceClusterSupportModelProperty());

      sourceDeclarer.withModelProperty(parser.getBackPressureStrategyModelProperty().orElse(getDefault()));

      sourceDeclarers.put(parser, sourceDeclarer);
    }
  }

  private void declareSourceCallbackParameters(Optional<SourceCallbackModelParser> parser,
                                               Supplier<ParameterizedDeclarer> declarer,
                                               ExtensionLoadingContext context) {
    parser.ifPresent(callback -> loader.getParameterModelsLoaderDelegate().declare(declarer.get(),
                                                                                   callback.getParameterGroupModelParsers(),
                                                                                   context));
  }

}
