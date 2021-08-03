/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclarer;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for declaring routers through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class RouterModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private final Map<OperationModelParser, ConstructDeclarer> constructDeclarers = new HashMap<>();

  RouterModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareRouter(ExtensionDeclarer extensionDeclarer, HasConstructDeclarer ownerDeclarer, OperationModelParser parser) {
    HasConstructDeclarer actualDeclarer = parser.hasConfig() || parser.isConnected()
        ? ownerDeclarer
        : extensionDeclarer;

    if (constructDeclarers.containsKey(parser)) {
      actualDeclarer.withConstruct(constructDeclarers.get(parser));
      return;
    }

    final ConstructDeclarer router = actualDeclarer.withConstruct(parser.getName())
        .describedAs(parser.getDescription())
        .withModelProperty(parser.getExecutorModelProperty());
    
    parser.getMediaTypeModelProperty().ifPresent(router::withModelProperty);
    parser.getAdditionalModelProperties().forEach(router::withModelProperty);

    loader.getParameterModelsLoaderDelegate().declare(router, parser.getParameterGroupModelParsers());

    declareRoutes(router, parser);

    constructDeclarers.put(parser, router);
  }

  private void declareRoutes(ConstructDeclarer router, OperationModelParser parser) {
    parser.getNestedRouteParsers().forEach(route -> {
      NestedRouteDeclarer routeDeclarer = router
          .withRoute(route.getName())
          .describedAs(route.getDescription())
          .withMinOccurs(route.getMinOccurs())
          .withMaxOccurs(route.getMaxOccurs().orElse(null));

      route.getAdditionalModelProperties().forEach(routeDeclarer::withModelProperty);
      loader.getParameterModelsLoaderDelegate().declare(routeDeclarer, parser.getParameterGroupModelParsers());
    });
  }
}
