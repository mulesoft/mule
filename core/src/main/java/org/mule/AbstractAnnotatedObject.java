/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.AnnotatedObject;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

/**
 * Base implementation for {@link AnnotatedObject}
 */
public abstract class AbstractAnnotatedObject implements AnnotatedObject
{

    private final Map<QName, Object> annotations = new ConcurrentHashMap<>();

    @Override
    public final Object getAnnotation(QName qName)
    {
        return annotations.get(qName);
    }

    @Override
    public final Map<QName, Object> getAnnotations()
    {
        return Collections.unmodifiableMap(annotations);
    }

    @Override
    public synchronized void setAnnotations(Map<QName, Object> newAnnotations)
    {
        annotations.clear();
        annotations.putAll(newAnnotations);
    }
}
