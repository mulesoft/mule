/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.scan.annotations;

import java.lang.annotation.Annotation;

/**
 * Simply filters for annotations that match the annotation class
 */
public class AnnotationTypeFilter implements AnnotationFilter
{
    private Class<? extends Annotation> annotation;

    public AnnotationTypeFilter(Class<? extends Annotation> annotation)
    {
        this.annotation = annotation;
    }

    public boolean accept(AnnotationInfo info)
    {
        return info.getClassName().equals(annotation.getName());
    }
}
