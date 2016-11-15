/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithParameters;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.ParameterizableTypeWrapper;
import org.mule.runtime.extension.api.model.property.ConfigTypeModelProperty;
import org.mule.runtime.extension.api.model.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * {@link ModelEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for annotated component
 * parameters (Sources and Operations), with {@link Connection} and adds a {@link ConnectivityModelProperty} or annotated with
 * {@link UseConfig} and adds a {@link ConfigTypeModelProperty}
 *
 * @since 4.0
 */
public class ConfigurationModelEnricher extends AbstractAnnotatedModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    final Optional<ImplementingTypeModelProperty> implementingType =
        extractExtensionType(describingContext.getExtensionDeclarer().getDeclaration());
    if (implementingType.isPresent()) {
      ClassTypeLoader typeLoader =
          ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());
      new IdempotentDeclarationWalker() {

        @Override
        protected void onOperation(OperationDeclaration declaration) {
          enrich(declaration, ImplementingMethodModelProperty.class,
                 (operation, property) -> enrich(operation, new MethodWrapper(property.getMethod()), typeLoader));
        }

        @Override
        public void onSource(SourceDeclaration declaration) {
          enrich(declaration, ImplementingTypeModelProperty.class,
                 (source, property) -> enrich(source, new ParameterizableTypeWrapper(property.getType()), typeLoader));
        }
      }.walk(describingContext.getExtensionDeclarer().getDeclaration());
    }
  }

  private <P extends ModelProperty> void enrich(BaseDeclaration declaration, Class<P> propertyType,
                                                BiConsumer<BaseDeclaration, P> consumer) {
    declaration.getModelProperty(propertyType).ifPresent(p -> consumer.accept(declaration, (P) p));
  }

  private void enrich(BaseDeclaration declaration, WithParameters methodWrapper, ClassTypeLoader typeLoader) {
    final List<ExtensionParameter> configParameters = methodWrapper.getParametersAnnotatedWith(UseConfig.class);
    if (!configParameters.isEmpty()) {
      declaration.addModelProperty(new ConfigTypeModelProperty(configParameters.get(0).getMetadataType(typeLoader)));
    }
  }
}
