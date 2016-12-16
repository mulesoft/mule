/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.tx.OperationTransactionalAction.JOIN_IF_POSSIBLE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_TAB_NAME;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

public final class TransactionalModelEnricher implements ModelEnricher {

  private final MetadataType transactionalActionType;

  public TransactionalModelEnricher() {
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    transactionalActionType = typeLoader.load(OperationTransactionalAction.class);
  }

  @Override
  public void enrich(DescribingContext describingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        if (declaration.isTransactional()) {
          declaration.getAllParameters().stream()
              .filter(parameter -> TRANSACTIONAL_ACTION_PARAMETER_NAME.equals(parameter.getName()))
              .findAny().ifPresent(p -> {
                throw new IllegalOperationModelDefinitionException(
                                                                   format("Operation '%s' from extension '%s' defines a parameter named '%s' which is a reserved word",
                                                                          declaration.getName(), describingContext
                                                                              .getExtensionDeclarer().getDeclaration().getName(),
                                                                          TRANSACTIONAL_ACTION_PARAMETER_NAME));
              });

          ParameterDeclaration transactionParameter = new ParameterDeclaration(TRANSACTIONAL_ACTION_PARAMETER_NAME);
          transactionParameter.setType(transactionalActionType, false);
          transactionParameter.setExpressionSupport(NOT_SUPPORTED);
          transactionParameter.setRequired(false);
          transactionParameter.setDefaultValue(JOIN_IF_POSSIBLE);
          transactionParameter.setDescription(TRANSACTIONAL_ACTION_PARAMETER_DESCRIPTION);
          transactionParameter.setLayoutModel(LayoutModel.builder().tabName(TRANSACTIONAL_TAB_NAME).build());

          declaration.getParameterGroup(DEFAULT_GROUP_NAME).addParameter(transactionParameter);
        }
      }
    }.walk(describingContext.getExtensionDeclarer().getDeclaration());
  }
}
