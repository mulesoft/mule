/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
