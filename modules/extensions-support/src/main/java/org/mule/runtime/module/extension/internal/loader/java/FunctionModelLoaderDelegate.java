/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.FunctionDeclarer;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for declaring functions through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class FunctionModelLoaderDelegate extends AbstractModelLoaderDelegate {


  private final Map<FunctionModelParser, FunctionDeclarer> functionDeclarers = new HashMap<>();

  FunctionModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareFunctions(ExtensionDeclarer extensionDeclarer, List<FunctionModelParser> parsers) {

    for (FunctionModelParser parser : parsers) {

      if (parser.isIgnored()) {
        continue;
      }

      if (functionDeclarers.containsKey(parser)) {
        extensionDeclarer.withFunction(functionDeclarers.get(parser));
        continue;
      }

      final FunctionDeclarer function = extensionDeclarer.withFunction(parser.getName())
          .describedAs(parser.getDescription())
          .withModelProperty(parser.getFunctionExecutorModelProperty());

      parser.getOutputType().applyOn(function.withOutput());
      loader.getParameterModelsLoaderDelegate().declare(function, parser.getParameterGroupModelParsers());
      parser.getAdditionalModelProperties().forEach(function::withModelProperty);

      functionDeclarers.put(parser, function);
    }
  }
}
