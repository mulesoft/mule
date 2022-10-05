/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.declareEmittedNotifications;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.declareMetadataResolverFactoryModelProperty;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.declareTypeResolversInformationModelProperty;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;
import static org.mule.sdk.api.metadata.NullMetadataResolver.NULL_RESOLVER_NAME;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.module.extension.internal.error.ErrorsModelFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.KeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaKeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class for declaring operations through a {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
final class OperationModelLoaderDelegate extends AbstractComponentModelLoaderDelegate {

  private final Map<OperationModelParser, OperationDeclarer> operationDeclarers = new HashMap<>();
  private final RouterModelLoaderDelegate routersDelegate;

  OperationModelLoaderDelegate(DefaultExtensionModelLoaderDelegate delegate) {
    super(delegate);
    routersDelegate = new RouterModelLoaderDelegate(delegate);
  }

  void declareOperations(ExtensionDeclarer extensionDeclarer,
                         HasOperationDeclarer ownerDeclarer,
                         List<OperationModelParser> operations) {

    for (OperationModelParser parser : operations) {

      if (parser.isIgnored()) {
        continue;
      }

      final boolean requiresConfig = requiresConfig(parser);
      HasOperationDeclarer actualDeclarer = requiresConfig
          ? ownerDeclarer
          : extensionDeclarer;
      final boolean extensionLevelOperation = actualDeclarer == extensionDeclarer;

      if (extensionLevelOperation && parser.isAutoPaging()) {
        throw new IllegalOperationModelDefinitionException(
                                                           format("Paged operation '%s' is defined at the extension level but it requires a config, "
                                                               + "since connections are required for paging", parser.getName()));
      }

      if (extensionLevelOperation && requiresConfig) {
        throw new IllegalOperationModelDefinitionException(format(
                                                                  "Operation '%s' is defined at the extension level but it requires a config. "
                                                                      + "Remove such parameter or move the operation to the proper config",
                                                                  parser.getName()));
      }

      if (operationDeclarers.containsKey(parser)) {
        actualDeclarer.withOperation(operationDeclarers.get(parser));
        continue;
      }

      if (parser.isRouter()) {
        routersDelegate.declareRouter(extensionDeclarer, (HasConstructDeclarer) ownerDeclarer, parser);
        continue;
      }

      final OperationDeclarer operation = actualDeclarer.withOperation(parser.getName())
          .describedAs(parser.getDescription())
          .supportsStreaming(parser.supportsStreaming())
          .transactional(parser.isTransactional())
          .requiresConnection(parser.isConnected())
          .blocking(parser.isBlocking())
          .withVisibility(parser.getComponentVisibility());

      parser.getExecutorModelProperty().ifPresent(operation::withModelProperty);
      parser.getOutputType().applyOn(operation.withOutput());
      parser.getAttributesOutputType().applyOn(operation.withOutputAttributes());
      parser.getMediaTypeModelProperty().ifPresent(operation::withModelProperty);

      Optional<OutputResolverModelParser> outputResolverModelParser = parser.getOutputResolverModelParser();
      Optional<AttributesResolverModelParser> attributesResolverModelParser = parser.getAttributesResolverModelParser();
      List<InputResolverModelParser> inputResolverModelParsers = parser.getInputResolverModelParsers();
      Optional<KeyIdResolverModelParser> keyIdResolverModelParser = getKeyIdResolverModelParser(parser, outputResolverModelParser
          .orElse(null),
                                                                                                attributesResolverModelParser
                                                                                                    .orElse(null),
                                                                                                inputResolverModelParsers);

      declareTypeResolversInformationModelProperty(operation.getDeclaration(), outputResolverModelParser,
                                                   attributesResolverModelParser, inputResolverModelParsers,
                                                   keyIdResolverModelParser, parser.isConnected());

      declareMetadataResolverFactoryModelProperty(operation.getDeclaration(), outputResolverModelParser,
                                                  attributesResolverModelParser,
                                                  inputResolverModelParsers, keyIdResolverModelParser);

      declareMetadataKeyIdModelProperty(operation, outputResolverModelParser, inputResolverModelParsers,
                                        keyIdResolverModelParser);

      parser.getDeprecationModel().ifPresent(operation::withDeprecation);
      parser.getDisplayModel().ifPresent(d -> operation.getDeclaration().setDisplayModel(d));
      parser.getSinceMuleVersionModelProperty().ifPresent(operation::withModelProperty);

      loader.getParameterModelsLoaderDelegate().declare(operation, keyIdResolverModelParser.orElse(null),
                                                        parser.getParameterGroupModelParsers());
      addSemanticTerms(operation.getDeclaration(), parser);
      parser.getExecutionType().ifPresent(operation::withExecutionType);
      parser.getAdditionalModelProperties().forEach(operation::withModelProperty);
      parser.getExceptionHandlerModelProperty().ifPresent(operation::withModelProperty);

      parser.getNestedChainParser().ifPresent(chain -> {
        NestedChainDeclarer chainDeclarer = operation.withChain(chain.getName())
            .describedAs(chain.getDescription())
            .setRequired(chain.isRequired());
        addSemanticTerms(chainDeclarer.getDeclaration(), chain);
        getStereotypeModelLoaderDelegate().addAllowedStereotypes(chain, chainDeclarer);
      });

      loader.registerOutputTypes(operation.getDeclaration());
      declareErrorModels(operation, parser, extensionDeclarer);
      getStereotypeModelLoaderDelegate().addStereotypes(
                                                        parser,
                                                        operation,
                                                        of(() -> getStereotypeModelLoaderDelegate()
                                                            .getDefaultOperationStereotype(parser.getName())));

      declareEmittedNotifications(parser, operation, loader::getNotificationModel);

      operationDeclarers.put(parser, operation);
    }
  }

  private void declareErrorModels(OperationDeclarer operation, OperationModelParser parser, ExtensionDeclarer extension) {
    final ErrorsModelFactory errorsModelFactory = loader.createErrorModelFactory();
    for (ErrorModelParser errorModelParser : parser.getErrorModelParsers()) {
      ErrorModel errorModel = errorsModelFactory.getErrorModel(errorModelParser);

      // Only the non-suppressed errors must appear in the operation model
      if (!errorModelParser.isSuppressed()) {
        operation.withErrorModel(errorModel);
      }

      // All the errors from all the operations will be declared in the extension, even if they are suppressed. The
      // ErrorTypeRepository is populated with the errors declared in the ExtensionModel, then without changing the API,
      // there is no way to avoid declaring them there.
      extension.withErrorModel(errorModel);
    }
  }

  private void declareMetadataKeyIdModelProperty(OperationDeclarer operation,
                                                 Optional<OutputResolverModelParser> outputResolverModelParser,
                                                 List<InputResolverModelParser> inputResolverModelParsers,
                                                 Optional<KeyIdResolverModelParser> keyIdResolverModelParser) {
    if (keyIdResolverModelParser.isPresent()) {
      String parameterName = keyIdResolverModelParser.get().getParameterName();
      MetadataType metadataType = keyIdResolverModelParser.get().getMetadataType();
      String categoryName = JavaModelLoaderUtils.getCategoryName(keyIdResolverModelParser.orElse(null), inputResolverModelParsers,
                                                                 outputResolverModelParser.orElse(null));

      operation.withModelProperty(new MetadataKeyIdModelProperty(metadataType, parameterName, categoryName));
    }
  }

  private Optional<KeyIdResolverModelParser> getKeyIdResolverModelParser(OperationModelParser operationModelParser,
                                                                         OutputResolverModelParser outputResolverModelParser,
                                                                         AttributesResolverModelParser attributesResolverModelParser,
                                                                         List<InputResolverModelParser> inputResolverModelParsers) {

    Optional<KeyIdResolverModelParser> keyIdResolverModelParser = empty();
    if (outputResolverModelParser != null || !inputResolverModelParsers.isEmpty()) {
      String categoryName = getCategoryName(outputResolverModelParser, attributesResolverModelParser, inputResolverModelParsers);

      keyIdResolverModelParser = operationModelParser.getParameterGroupModelParsers().stream()
          .map(parameterGroupModelParser -> parameterGroupModelParser.getKeyIdResolverModelParser(categoryName))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst();

      if (!keyIdResolverModelParser.isPresent()) {
        keyIdResolverModelParser = operationModelParser.getParameterGroupModelParsers().stream()
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
        Optional<KeyIdResolverModelParser> enclosingKeyIdResolverModelParser = operationModelParser.getKeyIdResolverModelParser();
        if (enclosingKeyIdResolverModelParser.isPresent()) {
          keyIdResolverModelParser = of(new JavaKeyIdResolverModelParser(
                                                                         keyIdResolverModelParser.get().getParameterName(),
                                                                         categoryName,
                                                                         keyIdResolverModelParser.get().getMetadataType(),
                                                                         enclosingKeyIdResolverModelParser.get()
                                                                             .keyIdResolverDeclarationClass()));
        }
      }
    }
    return keyIdResolverModelParser;
  }

  private String getCategoryName(OutputResolverModelParser outputResolverModelParser,
                                 AttributesResolverModelParser attributesResolverModelParser,
                                 List<InputResolverModelParser> inputResolverModelParsers) {

    if (outputResolverModelParser != null) {
      return outputResolverModelParser.getOutputResolver().getCategoryName();
    }

    if (attributesResolverModelParser != null) {
      return attributesResolverModelParser.getAttributesResolver().getCategoryName();
    }

    for (InputResolverModelParser inputResolverModelParser : inputResolverModelParsers) {
      return inputResolverModelParser.getInputResolver().getCategoryName();
    }

    throw new IllegalModelDefinitionException("Unable to create Keys Resolver. A Keys Resolver is being defined " +
        "without defining an Output Resolver, Input Resolver nor Attributes Resolver");
  }

}
