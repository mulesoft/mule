/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.model.display.LayoutModel.builderFrom;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * {@link DeclarationEnricher} which walks through all the {@link ParameterDeclaration} of the entire extension
 * and populates them with the correspondent order.
 *
 * @since 4.0
 * @see LayoutModel
 */
public final class ParameterLayoutOrderDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  private static final int INITIAL_ORDER = 1;

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();

    new IdempotentDeclarationWalker() {

      @Override
      public void onOperation(OperationDeclaration declaration) {
        addMissingParameterOrders(declaration);
      }

      @Override
      public void onConfiguration(ConfigurationDeclaration declaration) {
        addMissingParameterOrders(declaration);
      }

      @Override
      public void onSource(SourceDeclaration declaration) {
        addMissingParameterOrders(declaration);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        addMissingParameterOrders(declaration);
      }

    }.walk(declaration);
  }

  private void addMissingParameterOrders(ParameterizedDeclaration<?> declaration) {
    Set<Integer> takenOrders = new HashSet<>();
    List<ParameterDeclaration> nonOrderedParams = new ArrayList<>();

    declaration.getAllParameters().forEach(param -> {
      LayoutModel layoutModel = param.getLayoutModel();

      if (layoutModel.getOrder().isPresent()) {
        takenOrders.add(layoutModel.getOrder().get());
      } else {
        nonOrderedParams.add(param);
      }

    });

    AtomicInteger currentOrder = new AtomicInteger(INITIAL_ORDER);
    nonOrderedParams.forEach(param -> param.setLayoutModel(
                                                           builderFrom(param.getLayoutModel())
                                                               .order(getNextOrder(currentOrder, takenOrders))
                                                               .build()));
  }

  private int getNextOrder(AtomicInteger init, Set<Integer> takenOrders) {
    while (takenOrders.contains(init.get())) {
      init.incrementAndGet();
    }
    return init.getAndIncrement();
  }
}
