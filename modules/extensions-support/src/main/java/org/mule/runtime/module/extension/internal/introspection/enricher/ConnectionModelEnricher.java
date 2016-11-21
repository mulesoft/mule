/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.tx.OperationTransactionalAction.JOIN_IF_POSSIBLE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_TAB_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.addInterceptorFactory;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.model.property.ConnectivityModelProperty;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithParameters;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.ParameterizableTypeWrapper;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionInterceptor;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.util.List;
import java.util.Optional;

/**
 * Adds a {@link ConnectionInterceptor} to all {@link OperationModel operations} which contain the
 * {@link ConnectivityModelProperty}
 *
 * @since 4.0
 */
public class ConnectionModelEnricher extends AbstractAnnotatedModelEnricher {

  private final MetadataType transactionalActionType;
  private ClassTypeLoader typeLoader;


  public ConnectionModelEnricher() {
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    transactionalActionType = typeLoader.load(OperationTransactionalAction.class);
  }

  @Override
  public void enrich(DescribingContext describingContext) {
    final Optional<ImplementingTypeModelProperty> implementingType =
        extractExtensionType(describingContext.getExtensionDeclarer().getDeclaration());
    if (implementingType.isPresent()) {
      typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());
      new IdempotentDeclarationWalker() {

        @Override
        public void onOperation(OperationDeclaration declaration) {
          final Optional<ImplementingMethodModelProperty> implementingProperty =
              declaration.getModelProperty(ImplementingMethodModelProperty.class);

          if (implementingProperty.isPresent()) {
            final Optional<ConnectivityModelProperty> connectivityModelProperty =
                addModelProperty(declaration, new MethodWrapper(implementingProperty.get().getMethod()));
            connectivityModelProperty
                .ifPresent(connection -> addConnectionInterceptors(declaration, describingContext, connection));
          }
        }

        @Override
        public void onSource(SourceDeclaration declaration) {
          declaration.getModelProperty(ImplementingTypeModelProperty.class)
              .ifPresent(implementingProperty -> addModelProperty(declaration, new ParameterizableTypeWrapper(implementingProperty
                  .getType())));
        }
      }.walk(describingContext.getExtensionDeclarer().getDeclaration());
    }
  }

  private void addTransactionalActionParameter(DescribingContext describingContext, OperationDeclaration declaration) {
    declaration.getParameters().stream().filter(parameter -> TRANSACTIONAL_ACTION_PARAMETER_NAME.equals(parameter.getName()))
        .findAny().ifPresent(p -> {
          throw new IllegalOperationModelDefinitionException(format("Operation '%s' from extension '%s' defines a parameter named '%s' which is a reserved word",
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

    declaration.addParameter(transactionParameter);
  }

  private void addConnectionInterceptors(OperationDeclaration declaration, DescribingContext describingContext,
                                         ConnectivityModelProperty connection) {

    addInterceptorFactory(declaration, ConnectionInterceptor::new);
    if (connection.supportsTransactions()) {
      addTransactionalActionParameter(describingContext, declaration);
    }
  }

  private Optional<ConnectivityModelProperty> addModelProperty(BaseDeclaration declaration, WithParameters methodWrapper) {
    final List<ExtensionParameter> connectionParameters = methodWrapper.getParametersAnnotatedWith(Connection.class);
    if (!connectionParameters.isEmpty()) {
      ConnectivityModelProperty modelProperty =
          new ConnectivityModelProperty(connectionParameters.get(0).getMetadataType(typeLoader));
      declaration.addModelProperty(modelProperty);
      return Optional.of(modelProperty);
    }
    return Optional.empty();
  }
}
