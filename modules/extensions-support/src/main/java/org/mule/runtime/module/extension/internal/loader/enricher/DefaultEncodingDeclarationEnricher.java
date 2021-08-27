/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static com.google.common.base.Predicates.or;
import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.property.DefaultEncodingModelProperty;
import java.lang.reflect.Field;

import com.google.common.base.Predicate;

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
public final class DefaultEncodingDeclarationEnricher extends AbstractAnnotatedFieldDeclarationEnricher {

  @Override
  protected ModelProperty getModelProperty(Field field) {
    return new DefaultEncodingModelProperty(field);
  }

  @Override
  protected Predicate<Field> getFieldHasAnnotationPredicate() {
    return or(withAnnotation(DefaultEncoding.class), withAnnotation(org.mule.sdk.api.annotation.param.DefaultEncoding.class));
  }

  @Override
  protected String getAnnotationName() {
    return "DefaultEncoding";
  }

  protected Class getImplementingClass() {
    return String.class;
  }
}
