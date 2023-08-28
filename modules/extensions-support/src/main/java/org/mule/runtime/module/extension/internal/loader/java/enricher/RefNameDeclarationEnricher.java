/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static com.google.common.base.Predicates.or;
import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.IdempotentDeclarationEnricherWalkDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.RequireNameField;

import java.lang.reflect.Field;

import com.google.common.base.Predicate;

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
  protected DeclarationEnricherWalkDelegate getWalkDelegate(Predicate<Field> fieldHasAnnotationPredicate) {
    // feature not supported on sources
    return new IdempotentDeclarationEnricherWalkDelegate() {

      @Override
      public void onConfiguration(ConfigurationDeclaration declaration) {
        doEnrich(declaration, fieldHasAnnotationPredicate);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        doEnrich(declaration, fieldHasAnnotationPredicate);
      }
    };
  }

  @Override
  protected ModelProperty getModelProperty(Field field) {
    return new RequireNameField(field);
  }

  @Override
  protected Predicate<Field> getFieldHasAnnotationPredicate() {
    return or(withAnnotation(RefName.class), withAnnotation(org.mule.sdk.api.annotation.param.RefName.class));
  }

  @Override
  protected String getAnnotationName() {
    return "RefName";
  }

  @Override
  protected Class getImplementingClass() {
    return String.class;
  }
}
