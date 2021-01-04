/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.RequireNameField;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * A {@link DeclarationEnricher} which looks for configurations with fields annotated with {@link RefName}.
 * <p>
 * It validates that the annotations is used properly and if so it adds a {@link RequireNameField} on the
 * {@link ConfigurationDeclaration}.
 * <p>
 * If the {@link RefName} annotation is used in a way which breaks the rules set on its javadoc, an
 * {@link IllegalConfigurationModelDefinitionException} is thrown
 *
 * @since 4.0
 */
public final class RefNameDeclarationEnricher extends AbstractAnnotatedFieldDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      public void onConfiguration(ConfigurationDeclaration declaration) {
        doEnrich(declaration);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        doEnrich(declaration);
      }

    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  @Override
  protected void doEnrich(BaseDeclaration<?> declaration) {
    declaration.getModelProperty(ImplementingTypeModelProperty.class).ifPresent(typeProperty -> {
      Collection<Field> fields = getAllFields(typeProperty.getType(), withAnnotation(RefName.class));
      if (isEmpty(fields)) {
        return;
      }

      validate(fields, typeProperty, RefName.class, String.class);

      declaration.addModelProperty(new RequireNameField(fields.iterator().next()));
    });
  }
}
