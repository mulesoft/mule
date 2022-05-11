/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.property.RuntimeVersionModelProperty;
import org.mule.sdk.api.annotation.param.RuntimeVersion;

import java.lang.reflect.Field;

import com.google.common.base.Predicate;

/**
 * {@link DeclarationEnricher} which looks at classes with fields annotated with {@link RuntimeVersion}. It validates that the
 * annotations is used properly and if so it adds a {@link RuntimeVersionModelProperty}.
 * <p>
 * If the {@link RuntimeVersion} annotation is used in a way which breaks the rules set on its javadoc, an
 * {@link IllegalConfigurationModelDefinitionException} will be thrown.
 *
 * @since 4.4
 */
public class RuntimeVersionDeclarationEnricher extends AbstractAnnotatedFieldDeclarationEnricher {

  @Override
  protected ModelProperty getModelProperty(Field field) {
    return new RuntimeVersionModelProperty(field);
  }

  @Override
  protected Predicate<Field> getFieldHasAnnotationPredicate() {
    return withAnnotation(RuntimeVersion.class);
  }

  @Override
  protected String getAnnotationName() {
    return "RuntimeVersion";
  }

  @Override
  protected Class getImplementingClass() {
    return MuleVersion.class;
  }
}
