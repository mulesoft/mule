/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.POST_STRUCTURE;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.RequireNameField;

import java.lang.reflect.Field;
import java.util.Collection;

import com.google.common.base.Joiner;

/**
 * Base class for implementations of {@link DeclarationEnricher} that works on {@link ConfigurationDeclaration} and
 * {@link ConnectionProviderDeclaration} with fields annotated with the given annotation.
 *
 * @since 4.4
 */
public abstract class AbstractAnnotatedFieldDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return POST_STRUCTURE;
  }

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

  protected void doEnrich(BaseDeclaration<?> declaration) {
    Class annotationClass = getAnnotation();
    Class implementingType = getImplementingClass();

    declaration.getModelProperty(ImplementingTypeModelProperty.class).ifPresent(typeProperty -> {
      Collection<Field> fields = getAllFields(typeProperty.getType(), withAnnotation(annotationClass));
      if (isEmpty(fields)) {
        return;
      }

      validate(fields, typeProperty, annotationClass, implementingType);

      declaration.addModelProperty(getModelProperty(fields.iterator().next()));
    });
  }

  protected void validate(Collection<Field> fields, ImplementingTypeModelProperty typeProperty, Class annotation,
                          Class implementingClass) {
    if (fields.size() > 1) {
      throw new IllegalConfigurationModelDefinitionException(format("Only one field is allowed to be annotated" +
          "with @%s, but class '%s' has %d fields with such annotation. Offending fields are: [%s]",
                                                                    annotation.getSimpleName(),
                                                                    typeProperty.getType().getName(),
                                                                    fields.size(),
                                                                    Joiner.on(", ").join(fields.stream().map(Field::getName)
                                                                        .collect(toList()))));
    }

    final Field field = fields.iterator().next();
    if (!implementingClass.equals(field.getType())) {
      throw new IllegalConfigurationModelDefinitionException(format("Class '%s' declares the field '%s' which is" +
          "annotated with @%s and is of type '%s'. Only fields of type %s are allowed to carry such" +
          "annotation",
                                                                    typeProperty.getType().getName(),
                                                                    field.getName(),
                                                                    annotation.getSimpleName(),
                                                                    field.getType().getName(),
                                                                    implementingClass));
    }
  }

  protected abstract ModelProperty getModelProperty(Field field);

  protected abstract Class getAnnotation();

  protected abstract Class getImplementingClass();

}
