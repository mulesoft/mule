/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static java.lang.String.format;
import static java.util.Optional.of;

import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.asPagedOperation;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.withNoConnectivityError;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.withNoReconnectionStrategy;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.withNoStreamingConfiguration;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.withNoTransactionalAction;
import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.declareErrorModels;
import static org.mule.runtime.module.extension.internal.loader.ModelLoaderDelegateUtils.requiresConfig;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.declareEmittedNotifications;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.declareMetadataModelProperties;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.ExtensionDevelopmentFramework;
import org.mule.runtime.module.extension.internal.loader.java.property.SdkApiDefinedModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.OutputResolverModelParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for declaring operations through a {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
final class OperationModelLoaderDelegate extends AbstractComponentModelLoaderDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperationModelLoaderDelegate.class);

  private final Map<OperationModelParser, OperationDeclarer> operationDeclarers = new HashMap<>();

  OperationModelLoaderDelegate(DefaultExtensionModelLoaderDelegate delegate) {
    super(delegate);
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

      operationDeclarers.put(parser, createOperationDeclarer(parser, extensionDeclarer, actualDeclarer));
    }
  }

  private OperationDeclarer createOperationDeclarer(OperationModelParser parser, ExtensionDeclarer extensionDeclarer,
                                                    HasOperationDeclarer actualDeclarer) {
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

    declareMetadataModelProperties(operation.getDeclaration(),
                                   outputResolverModelParser,
                                   attributesResolverModelParser,
                                   inputResolverModelParsers,
                                   keyIdResolverModelParser,
                                   parser.isConnected(),
                                   parser.getScopeChainInputTypeResolverModelParser(),
                                   parser.getRoutesChainInputTypesResolverModelParser());

    parser.getDeprecationModel().ifPresent(operation::withDeprecation);
    parser.getDisplayModel().ifPresent(d -> operation.getDeclaration().setDisplayModel(d));
    if (parser.mustResolveMinMuleVersion()) {
      parser.getResolvedMinMuleVersion().ifPresent(resolvedMMV -> {
        operation.withMinMuleVersion(resolvedMMV.getMinMuleVersion());
        LOGGER.debug(resolvedMMV.getReason());
      });
    }
    loader.getParameterModelsLoaderDelegate().declare(operation, parser.getParameterGroupModelParsers());
    addSemanticTerms(operation.getDeclaration(), parser);
    parser.getExecutionType().ifPresent(operation::withExecutionType);

    if (parser.isAutoPaging()) {
      asPagedOperation(operation);
    }
    if (!parser.hasStreamingConfiguration()) {
      withNoStreamingConfiguration(operation);
    }
    if (!parser.hasTransactionalAction()) {
      withNoTransactionalAction(operation);
    }
    if (!parser.hasReconnectionStrategy()) {
      withNoReconnectionStrategy(operation);
    }
    if (!parser.propagatesConnectivityError()) {
      withNoConnectivityError(operation);
    }
    parser.getAdditionalModelProperties().forEach(operation::withModelProperty);
    parser.getExceptionHandlerModelProperty().ifPresent(operation::withModelProperty);

    declareChains(parser, operation);

    loader.registerOutputTypes(operation.getDeclaration());
    declareErrorModels(operation, parser, extensionDeclarer, loader.createErrorModelFactory());
    getStereotypeModelLoaderDelegate().addStereotypes(
                                                      parser,
                                                      operation,
                                                      of(() -> getStereotypeModelLoaderDelegate()
                                                          .getDefaultOperationStereotype(parser.getName())));

    declareEmittedNotifications(parser, operation, loader::getNotificationModel);

    return operation;
  }

  private void declareChains(OperationModelParser parser, OperationDeclarer operation) {
    if (parser.isScope()) {
      parser.getNestedChainParser().ifPresent(chain -> {
        NestedChainDeclarer chainDeclarer = operation.withChain(chain.getName())
            .describedAs(chain.getDescription())
            .setRequired(chain.isRequired())
            .setExecutionOccurrence(chain.getExecutionOccurrence());
        addSemanticTerms(chainDeclarer.getDeclaration(), chain);
        getStereotypeModelLoaderDelegate().addAllowedStereotypes(chain, chainDeclarer);
        if (chain.isSdkApiDefined()) {
          chainDeclarer.withModelProperty(SdkApiDefinedModelProperty.INSTANCE);
        }
      });
    } else if (parser.isRouter()) {
      parser.getNestedRouteParsers().forEach(route -> {
        NestedRouteDeclarer routeDeclarer = operation
            .withRoute(route.getName())
            .describedAs(route.getDescription())
            .withMinOccurs(route.getMinOccurs())
            .withMaxOccurs(route.getMaxOccurs().orElse(null));

        if (route.isSdkApiDefined()) {
          routeDeclarer.withModelProperty(SdkApiDefinedModelProperty.INSTANCE);
        }

        NestedChainDeclarer chain = routeDeclarer
            .withChain()
            .setExecutionOccurrence(route.getExecutionOccurrence());
        getStereotypeModelLoaderDelegate().addAllowedStereotypes(route, chain);

        route.getAdditionalModelProperties().forEach(routeDeclarer::withModelProperty);
        loader.getParameterModelsLoaderDelegate().declare(routeDeclarer, route.getParameterGroupModelParsers());
      });
    }
  }

}
