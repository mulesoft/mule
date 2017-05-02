/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Base class for implementations of {@link DeclarationEnricher} which provides utility methods for enriching {@link BaseDeclaration
 * declarations} constructed from annotated java classes
 *
 * @since 4.0
 */
public abstract class AbstractAnnotatedDeclarationEnricher implements DeclarationEnricher {

  /**
   * Extracts an {@link Annotation} from class backing the {@code declaration}.
   * <p/>
   * This method works in conjunction with {@link #extractImplementingTypeProperty(BaseDeclaration)}
   *
   * @param declaration    a {@link BaseDeclaration} to be enriched
   * @param annotationType the type of the annotation you want
   * @param <A>            the annotation's generic type
   * @return an {@link Annotation} or {@code null} if the annotation is not present or the {@code declaration} doesn't have a
   * backing annotated type
   */
  protected <A extends Annotation> A extractAnnotation(BaseDeclaration<? extends BaseDeclaration> declaration,
                                                       Class<A> annotationType) {
    Optional<ImplementingTypeModelProperty> implementingType = extractImplementingTypeProperty(declaration);
    return implementingType.isPresent() ? implementingType.get().getType().getAnnotation(annotationType) : null;
  }

  /**
   * Returns the annotated {@link Class} that was used to construct the {@code declaration}.
   * <p/>
   * The annotated type is determined by querying the {@code declaration} for the {@link ImplementingTypeModelProperty} model
   * property
   *
   * @param declaration a {@link BaseDeclaration} to be enriched
   * @return a {@link Class} or {@code null} if the model doesn't have a {@link ImplementingTypeModelProperty}
   */
  protected Optional<ImplementingTypeModelProperty> extractImplementingTypeProperty(
                                                                                    BaseDeclaration<? extends BaseDeclaration> declaration) {
    return declaration.getModelProperty(ImplementingTypeModelProperty.class);
  }

  /**
   * @param declaration a {@link BaseDeclaration}
   * @return The type from which the {@code declaration} was derived or {@link Optional#empty()} if it couldn't
   * be determined or the declaration is synthetic
   */
  protected Optional<Class> extractImplementingType(BaseDeclaration<? extends BaseDeclaration> declaration) {
    return extractImplementingTypeProperty(declaration).map(ImplementingTypeModelProperty::getType);
  }
}
