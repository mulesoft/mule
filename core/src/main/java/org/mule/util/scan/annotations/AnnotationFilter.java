/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan.annotations;

/**
 * A filter is used when performing an Annotation scan on a single class or the classpath.
 * An Annotation filter is used to filter throw annotations on a class (including fields, methods and parameters) to determine
 * whether the annotion should be accepted
 *
 * @deprecated: As ASM 3.3.1 is not fully compliant with Java 8, this class has been deprecated, however you can still use it under Java 7.
 */
@Deprecated
public interface AnnotationFilter
{
    /**
     * Should the annotation represented by an {@link AnnotationInfo} object be
     * accepted
     * 
     * @param info the annotation information to select on
     * @return true if the annotation matches the criteria for this filter
     */
    boolean accept(AnnotationInfo info);
}
