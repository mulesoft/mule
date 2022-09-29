/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.extension.api.property.BackPressureStrategyModelProperty.getDefault;
import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.declareEmittedNotifications;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;
import static org.mule.sdk.api.metadata.NullMetadataResolver.NULL_RESOLVER_NAME;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.KeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser.SourceCallbackModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaKeyIdResolverModelParser;

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
                             List<SourceModelParser> parsers) {

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
      parser.getSourceFactoryModelProperty().ifPresent(sourceDeclarer::withModelProperty);
      parser.getSinceMuleVersionModelProperty().ifPresent(sourceDeclarer::withModelProperty);

      parser.getOutputType().applyOn(sourceDeclarer.withOutput());
      parser.getAttributesOutputType().applyOn(sourceDeclarer.withOutputAttributes());

      Optional<OutputResolverModelParser> outputResolverModelParser = parser.getOutputResolverModelParser();
      Optional<AttributesResolverModelParser> attributesResolverModelParser = parser.getAttributesResolverModelParser();
      Optional<KeyIdResolverModelParser> keyIdResolverModelParser = getKeyIdResolverModelParser(parser,
                                                                                                outputResolverModelParser
                                                                                                    .orElse(null),
                                                                                                attributesResolverModelParser
                                                                                                    .orElse(null));

      loader.getParameterModelsLoaderDelegate().declare(sourceDeclarer, keyIdResolverModelParser.orElse(null),
                                                        parser.getParameterGroupModelParsers());

      parser.getMediaTypeModelProperty().ifPresent(sourceDeclarer::withModelProperty);
      parser.getExceptionHandlerModelProperty().ifPresent(sourceDeclarer::withModelProperty);
      loader.registerOutputTypes(sourceDeclarer.getDeclaration());

      declareTypeResolversInformationModelProperty(sourceDeclarer.getDeclaration(), outputResolverModelParser,
                                                   attributesResolverModelParser,
                                                   emptyList(), keyIdResolverModelParser, parser);

      declareMetadataResolverFactoryModelProperty(sourceDeclarer.getDeclaration(), outputResolverModelParser,
                                                  attributesResolverModelParser,
                                                  emptyList(), keyIdResolverModelParser);

      declareMetadataKeyIdModelProperty(sourceDeclarer, outputResolverModelParser, keyIdResolverModelParser);

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

        declareTypeResolversInformationModelProperty(onSuccessSourceCallbackDeclarer.getDeclaration(),
                                                     empty(), empty(), sourceCallbackInputResolverModelParsers, empty(), parser);

        declareMetadataResolverFactoryModelProperty(onSuccessSourceCallbackDeclarer.getDeclaration(), empty(), empty(),
                                                    sourceCallbackInputResolverModelParsers, empty());

        declareSourceCallbackParameters(successCallbackSourceCallbackModelParser, () -> onSuccessSourceCallbackDeclarer);
      }

      Optional<SourceCallbackModelParser> errorCallbackSourceCallbackModelParser = parser.getOnErrorCallbackParser();
      if (errorCallbackSourceCallbackModelParser.isPresent()) {
        SourceCallbackDeclarer onErrorSourceCallbackDeclarer = sourceDeclarer.onError();

        List<InputResolverModelParser> sourceCallbackInputResolverModelParsers =
            errorCallbackSourceCallbackModelParser.get().getInputResolverModelParsers();

        declareTypeResolversInformationModelProperty(onErrorSourceCallbackDeclarer.getDeclaration(),
                                                     empty(), empty(), sourceCallbackInputResolverModelParsers, empty(), parser);

        declareMetadataResolverFactoryModelProperty(onErrorSourceCallbackDeclarer.getDeclaration(), empty(), empty(),
                                                    sourceCallbackInputResolverModelParsers, empty());

        declareSourceCallbackParameters(parser.getOnErrorCallbackParser(), () -> onErrorSourceCallbackDeclarer);
      }

      // TODO: MULE-9220 add syntax validator to check that none of these use @UseConfig or @Connection
      declareSourceCallbackParameters(parser.getOnTerminateCallbackParser(), sourceDeclarer::onTerminate);
      declareSourceCallbackParameters(parser.getOnBackPressureCallbackParser(), sourceDeclarer::onBackPressure);

      sourceDeclarer.withModelProperty(parser.getSourceClusterSupportModelProperty());

      sourceDeclarer.withModelProperty(parser.getBackPressureStrategyModelProperty().orElse(getDefault()));

      sourceDeclarers.put(parser, sourceDeclarer);
    }
  }


  private void declareSourceCallbackParameters(Optional<SourceCallbackModelParser> parser,
                                               Supplier<ParameterizedDeclarer> declarer) {
    parser.ifPresent(callback -> loader.getParameterModelsLoaderDelegate().declare(declarer.get(),
                                                                                   callback.getParameterGroupModelParsers()));
  }

  private void declareTypeResolversInformationModelProperty(BaseDeclaration baseDeclaration,
                                                            Optional<OutputResolverModelParser> outputResolverModelParser,
                                                            Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                            List<InputResolverModelParser> inputResolverModelParsers,
                                                            Optional<KeyIdResolverModelParser> keyIdResolverModelParser,
                                                            SourceModelParser sourceModelParser) {
    if ((outputResolverModelParser.isPresent() && outputResolverModelParser.get().hasOutputResolver()) ||
        !inputResolverModelParsers.isEmpty()) {

      Map<String, String> inputResolversByParam = inputResolverModelParsers.stream()
          .collect(toImmutableMap(InputResolverModelParser::getParameterName, r -> r.getInputResolver().getResolverName()));

      String outputResolver = outputResolverModelParser
          .map(outputResolverParser -> outputResolverParser.getOutputResolver().getResolverName())
          .orElse(NULL_RESOLVER_NAME);

      String attributesResolver = attributesResolverModelParser
          .map(attributesResolverParser -> attributesResolverParser.getAttributesResolver().getResolverName())
          .orElse(NULL_RESOLVER_NAME);

      String keysResolver = keyIdResolverModelParser
          .map(keyResolverParser -> keyResolverParser.getKeyResolver().getResolverName())
          .orElse(NULL_RESOLVER_NAME);

      boolean isPartialKeyResolver = keyIdResolverModelParser
          .map(KeyIdResolverModelParser::isPartialKeyResolver).orElse(false);

      boolean requiresConnection = sourceModelParser.isConnected();

      String categoryName = getCategoryName(keyIdResolverModelParser.orElse(null), inputResolverModelParsers,
                                            outputResolverModelParser.orElse(null));

      TypeResolversInformationModelProperty typeResolversInformationModelProperty = new TypeResolversInformationModelProperty(
                                                                                                                              categoryName,
                                                                                                                              inputResolversByParam,
                                                                                                                              outputResolver,
                                                                                                                              attributesResolver,
                                                                                                                              keysResolver,
                                                                                                                              requiresConnection,
                                                                                                                              requiresConnection,
                                                                                                                              isPartialKeyResolver);

      baseDeclaration.addModelProperty(typeResolversInformationModelProperty);
    }
  }

  private void declareMetadataResolverFactoryModelProperty(BaseDeclaration baseDeclaration,
                                                           Optional<OutputResolverModelParser> outputResolverModelParser,
                                                           Optional<AttributesResolverModelParser> attributesResolverModelParser,
                                                           List<InputResolverModelParser> inputResolverModelParsers,
                                                           Optional<KeyIdResolverModelParser> keyIdResolverModelParser) {
    MetadataResolverFactory metadataResolverFactory;
    if ((outputResolverModelParser.isPresent() && outputResolverModelParser.get().hasOutputResolver()) ||
        !inputResolverModelParsers.isEmpty()) {

      NullMetadataResolver nullMetadataResolver = new NullMetadataResolver();

      OutputTypeResolver<?> outputTypeResolver = outputResolverModelParser.map(OutputResolverModelParser::getOutputResolver)
          .orElse((OutputTypeResolver) nullMetadataResolver);
      Supplier<OutputTypeResolver<?>> outputTypeResolverSupplier = () -> outputTypeResolver;

      AttributesTypeResolver<?> attributesTypeResolver =
          attributesResolverModelParser.map(AttributesResolverModelParser::getAttributesResolver)
              .orElse((AttributesTypeResolver) nullMetadataResolver);
      Supplier<AttributesTypeResolver<?>> attributesTypeResolverSupplier = () -> attributesTypeResolver;

      TypeKeysResolver typeKeysResolver = keyIdResolverModelParser.map(KeyIdResolverModelParser::getKeyResolver)
          .orElse(nullMetadataResolver);
      Supplier<TypeKeysResolver> typeKeysResolverSupplier = () -> typeKeysResolver;

      Map<String, Supplier<? extends InputTypeResolver>> inputTypeResolvers = new HashMap<>();
      inputResolverModelParsers.forEach(parser -> inputTypeResolvers.put(parser.getParameterName(), parser::getInputResolver));

      metadataResolverFactory = new DefaultMetadataResolverFactory(typeKeysResolverSupplier, inputTypeResolvers,
                                                                   outputTypeResolverSupplier,
                                                                   attributesTypeResolverSupplier);
    } else {
      metadataResolverFactory = new NullMetadataResolverFactory();
    }

    baseDeclaration.addModelProperty(new MetadataResolverFactoryModelProperty(() -> metadataResolverFactory));
  }

  private void declareMetadataKeyIdModelProperty(SourceDeclarer sourceDeclarer,
                                                 Optional<OutputResolverModelParser> outputResolverModelParser,
                                                 Optional<KeyIdResolverModelParser> keyIdResolverModelParser) {
    if (keyIdResolverModelParser.isPresent()) {
      String parameterName = keyIdResolverModelParser.get().getParameterName();
      MetadataType metadataType = keyIdResolverModelParser.get().getMetadataType();
      String categoryName =
          getCategoryName(keyIdResolverModelParser.orElse(null), emptyList(), outputResolverModelParser.orElse(null));

      sourceDeclarer.withModelProperty(new MetadataKeyIdModelProperty(metadataType, parameterName, categoryName));
    }
  }

  private Optional<KeyIdResolverModelParser> getKeyIdResolverModelParser(SourceModelParser sourceModelParser,
                                                                         OutputResolverModelParser outputResolverModelParser,
                                                                         AttributesResolverModelParser attributesResolverModelParser) {
    Optional<KeyIdResolverModelParser> keyIdResolverModelParser = empty();
    if (outputResolverModelParser != null) {
      String categoryName = getCategoryName(outputResolverModelParser, attributesResolverModelParser);

      keyIdResolverModelParser = sourceModelParser.getParameterGroupModelParsers().stream()
          .map(parameterGroupModelParser -> parameterGroupModelParser.getKeyIdResolverModelParser(categoryName))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst();

      if (!keyIdResolverModelParser.isPresent()) {
        keyIdResolverModelParser = sourceModelParser.getParameterGroupModelParsers().stream()
            .map(ParameterGroupModelParser::getParameterParsers)
            .flatMap(List::stream)
            .collect(toList())
            .stream()
            .map(parameterModelParser -> parameterModelParser.getKeyIdResolverModelParser(categoryName))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
      }

      if (keyIdResolverModelParser.isPresent() && !keyIdResolverModelParser.get().hasKeyIdResolver()) {
        Optional<KeyIdResolverModelParser> enclosingKeyIdResolverModelParser = sourceModelParser.getKeyIdResolverModelParser();
        if (enclosingKeyIdResolverModelParser.isPresent()) {
          keyIdResolverModelParser = of(new JavaKeyIdResolverModelParser(keyIdResolverModelParser.get().getParameterName(),
                                                                         categoryName,
                                                                         keyIdResolverModelParser.get().getMetadataType(),
                                                                         enclosingKeyIdResolverModelParser.get()
                                                                             .keyIdResolverDeclarationClass()));
        }
      }
    }

    return keyIdResolverModelParser;
  }

  private String getCategoryName(KeyIdResolverModelParser keyIdResolverModelParser,
                                 List<InputResolverModelParser> inputResolverModelParsers,
                                 OutputResolverModelParser outputResolverModelParser) {
    if (keyIdResolverModelParser != null && keyIdResolverModelParser.hasKeyIdResolver()) {
      return keyIdResolverModelParser.getKeyResolver().getCategoryName();
    } else if (!inputResolverModelParsers.isEmpty()) {
      return inputResolverModelParsers.iterator().next().getInputResolver().getCategoryName();
    } else if (outputResolverModelParser != null && outputResolverModelParser.hasOutputResolver()) {
      return outputResolverModelParser.getOutputResolver().getCategoryName();
    } else {
      return null;
    }
  }

  private String getCategoryName(OutputResolverModelParser outputResolverModelParser,
                                 AttributesResolverModelParser attributesResolverModelParser) {
    if (outputResolverModelParser != null) {
      return outputResolverModelParser.getOutputResolver().getCategoryName();
    }

    if (attributesResolverModelParser != null) {
      return attributesResolverModelParser.getAttributesResolver().getCategoryName();
    }


    throw new IllegalModelDefinitionException("Unable to create Keys Resolver. A Keys Resolver is being defined " +
        "without defining an Output Resolver, Input Resolver nor Attributes Resolver");
  }
}
