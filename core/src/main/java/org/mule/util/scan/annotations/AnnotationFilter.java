/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.scan.annotations;

/**
 * A filter is used when performing an Annotation scan on a single class or the classpath.
 * An Annotation filter is used to filter throw annotations on a class (including fields, methods and parameters) to determine
 * whether the annotion should be accepted
 */
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
