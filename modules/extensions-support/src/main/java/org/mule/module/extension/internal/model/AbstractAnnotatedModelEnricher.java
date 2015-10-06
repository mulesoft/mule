/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model;

import org.mule.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Base class for implementations of {@link ModelEnricher} which provides utility methods
 * for enriching {@link BaseDeclaration declarations} constructed from an annotated model
 *
 * @see AnnotationsBasedDescriber
 * @since 4.0
 */
public abstract class AbstractAnnotatedModelEnricher implements ModelEnricher
{

    /**
     * Extracts an {@link Annotation} from membar backing the {@code declaration}.
     * <p>
     * If the desired annotation is at a {@link Class} level, then this method works in conjunction
     * with {@link #extractExtensionType(BaseDeclaration)}. If the annotation acts at a {@link Method}
     * level, then the declaration is tested for the {@link ImplementingMethodModelProperty} property in
     * order to query the corresponding {@link Method}
     *
     * @param declaration    a {@link BaseDeclaration} to be enriched
     * @param annotationType the type of the annotation you want
     * @param <A>            the annotation's generic type
     * @return an {@link Annotation} or {@code null} if the annotation is not present or the {@code declaration} doesn't
     * have a backing annotated type
     */
    protected <A extends Annotation> A extractAnnotation(BaseDeclaration<? extends BaseDeclaration> declaration, Class<A> annotationType)
    {
        Class<?> extensionType = extractExtensionType(declaration);
        if (extensionType != null)
        {
            return extensionType.getAnnotation(annotationType);
        }

        ImplementingMethodModelProperty methodProperty = declaration.getModelProperty(ImplementingMethodModelProperty.KEY);
        if (methodProperty != null)
        {
            Method method = methodProperty.getMethod();
            return method.getAnnotation(annotationType);
        }

        return null;
    }

    /**
     * Returns the annotated {@link Class} that was used to construct the {@code declaration}.
     * <p>
     * The annotated type is determined by querying the {@code declaration} for the
     * {@link ImplementingTypeModelProperty} model property
     *
     * @param declaration a {@link BaseDeclaration} to be enriched
     * @param <T>         the return class's generic type
     * @return a {@link Class} or {@code null} if the model doesn't have a {@link ImplementingTypeModelProperty}
     */
    protected <T> Class<T> extractExtensionType(BaseDeclaration<? extends BaseDeclaration> declaration)
    {
        ImplementingTypeModelProperty backingType = declaration.getModelProperty(ImplementingTypeModelProperty.KEY);
        return backingType != null ? (Class<T>) backingType.getType() : null;
    }
}
