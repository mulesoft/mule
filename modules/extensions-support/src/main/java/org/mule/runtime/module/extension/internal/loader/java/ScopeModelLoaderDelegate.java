/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.java.OperationModelLoaderDelegate.checkDefinition;
import static org.mule.runtime.module.extension.internal.loader.java.OperationModelLoaderDelegate.processNonBlockingOperation;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isNonBlocking;

import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for declaring scopes through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class ScopeModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String SCOPE = "Scope";

  private final Map<OperationModelParser, OperationDeclarer> operationDeclarers = new HashMap<>();

  ScopeModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareScope(ExtensionDeclarer extensionDeclarer,
                    HasOperationDeclarer declarer,
                    OperationModelParser parser,
                    ExtensionLoadingContext loaderContext) {

    HasOperationDeclarer actualDeclarer = parser.hasConfig() || parser.isConnected()
        ? declarer
        : extensionDeclarer;

    checkDefinition(!parser.hasConfig(),
        format("Scope '%s' requires a config, but that is not allowed, remove such parameter",
            parser.getName()));

    checkDefinition(!parser.isConnected(),
        format("Scope '%s' requires a connection, but that is not allowed, remove such parameter",
            parser.getName()));


    if (operationDeclarers.containsKey(parser)) {
      actualDeclarer.withOperation(operationDeclarers.get(parser));
      return;
    }

    final OperationDeclarer scope = actualDeclarer.withOperation(parser.getName());
    parser.getAdditionalModelProperties().forEach(scope::withModelProperty);

    parser.getMediaTypeModelProperty().ifPresent(scope::withModelProperty);
    processNonBlockingOperation(scope, scopeMethod, false);

    List<ExtensionParameter> processorChain =
        scopeMethod.getParameters().stream().filter(ModelLoaderUtils::isProcessorChain).collect(toList());

    checkDefinition(processorChain.size() <= 1,
        format("Scope '%s' declares too many parameters of type '%s', only one input of this kind is supported."
                + "Offending parameters are: %s",
            scopeMethod.getAlias(),
            Chain.class.getSimpleName(),
            processorChain.stream().map(ExtensionParameter::getName).collect(toList())));

    declareParameters(scope, scopeMethod.getParameters(), scopeMethod.getEnclosingType().getParameters(),
        new ParameterDeclarationContext(SCOPE, scope.getDeclaration()));

    loader.addExceptionEnricher(scopeMethod, scope);

    operationDeclarers.put(scopeMethod, scope);
  }

}
