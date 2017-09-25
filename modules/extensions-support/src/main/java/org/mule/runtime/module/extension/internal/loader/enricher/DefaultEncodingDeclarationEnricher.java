/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.DefaultEncodingModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import com.google.common.base.Joiner;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * A {@link DeclarationEnricher} which looks classes with fields annotated with {@link DefaultEncoding}.
 * <p>
 * It validates that the annotations is used properly and if so it adds a {@link DefaultEncodingModelProperty}.
 * <p>
 * If the {@link DefaultEncoding} annotation is used in a way which breaks the rules set on its javadoc, an
 * {@link IllegalConfigurationModelDefinitionException} will be thrown
 *
 * @since 4.0
 */
public final class DefaultEncodingDeclarationEnricher implements DeclarationEnricher {

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

      @Override
      protected void onSource(SourceDeclaration declaration) {
        doEnrich(declaration);
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private void doEnrich(BaseDeclaration declaration) {
    declaration.getModelProperty(ImplementingTypeModelProperty.class).ifPresent(p -> {
      ImplementingTypeModelProperty typeProperty = (ImplementingTypeModelProperty) p;
      Collection<Field> fields = getAllFields(typeProperty.getType(), withAnnotation(DefaultEncoding.class));
      if (isEmpty(fields)) {
        return;
      }

      // TODO: MULE-9220 - Add a syntax validator which checks that the annotated parameter is a String
      if (fields.size() > 1) {
        throw new IllegalConfigurationModelDefinitionException(String
            .format("Only one field is allowed to be annotated with @%s, but class '%s' has %d fields "
                + "with such annotation. Offending fields are: [%s]", DefaultEncoding.class.getSimpleName(),
                    typeProperty.getType()
                        .getName(),
                    fields.size(), Joiner.on(", ").join(fields.stream().map(Field::getName).collect(toList()))));
      }

      final Field defaultEncodingField = fields.iterator().next();
      if (!String.class.equals(defaultEncodingField.getType())) {
        throw new IllegalConfigurationModelDefinitionException(String
            .format("Class '%s' declares the field '%s' which is annotated with @%s and is of type '%s'. Only "
                + "fields of type String are allowed to carry such annotation", typeProperty.getType().getName(),
                    defaultEncodingField.getName(), DefaultEncoding.class.getSimpleName(),
                    defaultEncodingField.getType().getName()));
      }

      declaration.addModelProperty(new DefaultEncodingModelProperty(defaultEncodingField));
    });
  }
}
