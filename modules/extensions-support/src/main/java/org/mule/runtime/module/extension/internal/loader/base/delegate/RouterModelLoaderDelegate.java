/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.base.delegate;

import static java.util.Optional.of;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;

import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclarer;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for declaring routers through a {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
final class RouterModelLoaderDelegate extends AbstractComponentModelLoaderDelegate {

  private final Map<OperationModelParser, ConstructDeclarer> constructDeclarers = new HashMap<>();

  RouterModelLoaderDelegate(DefaultExtensionModelLoaderDelegate delegate) {
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
        .describedAs(parser.getDescription());

    parser.getDeprecationModel().ifPresent(router::withDeprecation);
    parser.getExecutorModelProperty().ifPresent(router::withModelProperty);
    parser.getMediaTypeModelProperty().ifPresent(router::withModelProperty);
    parser.getAdditionalModelProperties().forEach(router::withModelProperty);

    loader.getParameterModelsLoaderDelegate().declare(router, parser.getParameterGroupModelParsers());
    addSemanticTerms(router.getDeclaration(), parser);
    declareRoutes(router, parser);

    getStereotypeModelLoaderDelegate().addStereotypes(
                                                      parser,
                                                      router,
                                                      of(() -> getStereotypeModelLoaderDelegate()
                                                          .getDefaultOperationStereotype(parser.getName())));

    constructDeclarers.put(parser, router);
  }

  private void declareRoutes(ConstructDeclarer router, OperationModelParser parser) {
    parser.getNestedRouteParsers().forEach(route -> {
      NestedRouteDeclarer routeDeclarer = router
          .withRoute(route.getName())
          .describedAs(route.getDescription())
          .withMinOccurs(route.getMinOccurs())
          .withMaxOccurs(route.getMaxOccurs().orElse(null));

      NestedChainDeclarer chain = routeDeclarer.withChain();
      getStereotypeModelLoaderDelegate().addAllowedStereotypes(route, chain);
      route.getAdditionalModelProperties().forEach(routeDeclarer::withModelProperty);
      loader.getParameterModelsLoaderDelegate().declare(routeDeclarer, route.getParameterGroupModelParsers());
    });
  }
}
