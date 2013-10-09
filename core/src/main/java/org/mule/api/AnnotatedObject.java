/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api;

import java.util.Map;

import javax.xml.namespace.QName;

public interface AnnotatedObject
{
    public static final String PROPERTY_NAME = "annotations";

    /**
     * Gets the value of specified annotation.
     * 
     * @return the value of specified annotation
     */
    Object getAnnotation(QName name);

    /**
     * Gets all annotations.
     *
     * @return all annotation
     */
    Map<QName, Object> getAnnotations();

    /**
     * Sets annotations to the object.
     */
    void setAnnotations(Map<QName, Object> annotations);
}