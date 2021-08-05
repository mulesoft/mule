/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for declaring operations through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class OperationModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private final Map<OperationModelParser, OperationDeclarer> operationDeclarers = new HashMap<>();
  private final RouterModelLoaderDelegate routersDelegate;

  OperationModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
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

      final boolean requiresConfig = parser.hasConfig() || parser.isConnected() || parser.isAutoPaging();
      HasOperationDeclarer actualDeclarer = requiresConfig
          ? ownerDeclarer
          : extensionDeclarer;
      final boolean extensionLevelOperation = actualDeclarer == extensionDeclarer;

      if (extensionLevelOperation && parser.isAutoPaging()) {
        throw new IllegalOperationModelDefinitionException(
                                                           format("Paged operation '%s' is defined at the extension level but it requires a config, "
                                                               + "since connections are required for paging", parser.getName()));
      }

      if (actualDeclarer == extensionDeclarer && requiresConfig) {
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
          .withModelProperty(parser.getExecutorModelProperty());

      parser.getOutputType().applyOn(operation.withOutput());
      parser.getAttributesOutputType().applyOn(operation.withOutputAttributes());
      parser.getMediaTypeModelProperty().ifPresent(operation::withModelProperty);

      loader.getParameterModelsLoaderDelegate().declare(operation, parser.getParameterGroupModelParsers());
      parser.getExecutionType().ifPresent(operation::withExecutionType);
      parser.getAdditionalModelProperties().forEach(operation::withModelProperty);
      parser.getExceptionHandlerModelProperty().ifPresent(operation::withModelProperty);

      parser.getNestedChainParser().ifPresent(chain -> operation.withChain(chain.getName())
          .describedAs(chain.getDescription())
          .setRequired(chain.isRequired()));

      operationDeclarers.put(parser, operation);
    }
  }
}
