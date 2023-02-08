/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.declareErrorModels;
import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.declareEmittedNotifications;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.declareMetadataResolverFactoryModelProperty;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.declareOperationMetadataKeyIdModelProperty;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.declareTypeResolversInformationModelProperty;

import static java.lang.String.format;
import static java.util.Optional.of;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.internal.ExtensionDevelopmentFramework;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.MetadataKeyModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mule.runtime.module.extension.internal.loader.parser.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for declaring operations through a {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
final class OperationModelLoaderDelegate extends AbstractComponentModelLoaderDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperationModelLoaderDelegate.class);

  private final Map<OperationModelParser, OperationDeclarer> operationDeclarers = new HashMap<>();
  private final RouterModelLoaderDelegate routersDelegate;

  OperationModelLoaderDelegate(DefaultExtensionModelLoaderDelegate delegate) {
    super(delegate);
    routersDelegate = new RouterModelLoaderDelegate(delegate);
  }

  void declareOperations(ExtensionDeclarer extensionDeclarer,
                         ExtensionDevelopmentFramework extensionDevelopmentFramework,
                         HasOperationDeclarer ownerDeclarer,
                         List<OperationModelParser> operations) {

    for (OperationModelParser parser : operations) {

      if (parser.isIgnored()) {
        continue;
      }

      final boolean requiresConfig = requiresConfig(extensionDevelopmentFramework, parser);
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
        routersDelegate.declareRouter(extensionDeclarer, extensionDevelopmentFramework, (HasConstructDeclarer) ownerDeclarer,
                                      parser);
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
      Optional<MetadataKeyModelParser> keyIdResolverModelParser = parser.getMetadataKeyModelParser();

      declareTypeResolversInformationModelProperty(operation.getDeclaration(), outputResolverModelParser,
                                                   attributesResolverModelParser, inputResolverModelParsers,
                                                   keyIdResolverModelParser, parser.isConnected());

      declareMetadataResolverFactoryModelProperty(operation.getDeclaration(), outputResolverModelParser,
                                                  attributesResolverModelParser,
                                                  inputResolverModelParsers, keyIdResolverModelParser);

      declareOperationMetadataKeyIdModelProperty(operation, outputResolverModelParser, inputResolverModelParsers,
                                                 keyIdResolverModelParser);

      parser.getDeprecationModel().ifPresent(operation::withDeprecation);
      parser.getDisplayModel().ifPresent(d -> operation.getDeclaration().setDisplayModel(d));
      parser.getResolvedMinMuleVersion().ifPresent(resolvedMMV -> {
        operation.withMinMuleVersion(resolvedMMV.getMinMuleVersion());
        LOGGER.debug(resolvedMMV.getReason());
      });
      loader.getParameterModelsLoaderDelegate().declare(operation, parser.getParameterGroupModelParsers());
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
      declareErrorModels(operation, parser, extensionDeclarer, loader.createErrorModelFactory());
      getStereotypeModelLoaderDelegate().addStereotypes(
                                                        parser,
                                                        operation,
                                                        of(() -> getStereotypeModelLoaderDelegate()
                                                            .getDefaultOperationStereotype(parser.getName())));

      declareEmittedNotifications(parser, operation, loader::getNotificationModel);

      operationDeclarers.put(parser, operation);
    }
  }

}
