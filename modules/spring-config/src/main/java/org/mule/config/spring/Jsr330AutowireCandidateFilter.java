/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Qualifier;

import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.MethodParameter;

/**
 * A workaround class for <a href="https://jira.spring.io/browse/SPR-12914">
 * Spring issue SPR-12914</a>.
 * <p/>
 * This class takes injection candidtes and filters them by considering the
 * {@link Named} and {@link Qualifier} annotations that might be present on the
 * injection points. If the injection point doesn't contain any of such annotations,
 * then the candidates list is not modified.
 * <p/>
 * When the Spring issue is fixed, this functionality should be removed from mule
 *
 * @since 3.7.0
 */
final class Jsr330AutowireCandidateFilter
{

    Map<String, Object> filter(Map<String, Object> candidates, DependencyDescriptor descriptor)
    {
        if (candidates.isEmpty())
        {
            return candidates;
        }

        if (!filterByName(candidates, descriptor))
        {
            filterByQualifier(candidates, descriptor);
        }

        return candidates;
    }

    private boolean filterByName(Map<String, Object> candidates, DependencyDescriptor descriptor)
    {
        AccessibleObject annotationSource = getAnnotationSource(descriptor);
        if (annotationSource == null)
        {
            return false;
        }

        Named namedAnnotation = annotationSource.getAnnotation(Named.class);
        if (namedAnnotation != null)
        {
            Object candidate = candidates.get(namedAnnotation.value());
            candidates.clear();
            if (candidate != null)
            {
                candidates.put(namedAnnotation.value(), candidate);
            }

            return true;
        }

        return false;
    }

    private boolean filterByQualifier(Map<String, Object> candidates, DependencyDescriptor descriptor)
    {
        Class<Annotation> qualifierType = getQualifierAnnotationType(descriptor);
        if (qualifierType == null)
        {
            return false;
        }

        Map.Entry<String, Object> match = null;

        for (Map.Entry<String, Object> candidate : candidates.entrySet())
        {
            Object candidatevalue = candidate.getValue();
            if (candidatevalue != null && candidatevalue.getClass().isAnnotationPresent(qualifierType))
            {
                match = candidate;
                break;
            }
        }

        candidates.clear();
        if (match != null)
        {
            candidates.put(match.getKey(), match.getValue());
        }

        return true;
    }


    private AccessibleObject getAnnotationSource(DependencyDescriptor descriptor)
    {
        AccessibleObject annotationSource = descriptor.getField();
        if (annotationSource == null)
        {
            MethodParameter methodParameter = descriptor.getMethodParameter();
            if (methodParameter != null)
            {
                annotationSource = methodParameter.getMethod();
            }
        }

        return annotationSource;
    }

    private Class<Annotation> getQualifierAnnotationType(DependencyDescriptor descriptor)
    {
        AccessibleObject annotationSource = getAnnotationSource(descriptor);
        if (annotationSource != null)
        {
            for (Annotation annotation : annotationSource.getAnnotations())
            {
                if (annotation.annotationType().isAnnotationPresent(Qualifier.class))
                {
                    return (Class<Annotation>) ClassUtils.resolveAnnotationClass(annotation);
                }
            }
        }

        return null;
    }
}
