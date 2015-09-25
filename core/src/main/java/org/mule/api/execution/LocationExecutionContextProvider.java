/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.execution;

import org.mule.api.AnnotatedObject;

import javax.xml.namespace.QName;

/**
 * Provides a standard way to generate a log entry or message that references an element in a flow.
 * 
 * @since 3.8.0
 */
public abstract class LocationExecutionContextProvider implements ExceptionContextProvider
{

    private static final QName NAME_ANNOTATION_KEY = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");

    /**
     * Generates a representation of a flow element to be logged in a standard way.
     * 
     * @param appId
     * @param processorPath
     * @param element
     * @return
     */
    public static String resolveProcessorRepresentation(String appId, String processorPath, Object element)
    {
        if (element instanceof AnnotatedObject)
        {
            Object docName = ((AnnotatedObject) element).getAnnotation(NAME_ANNOTATION_KEY);
            if (docName != null)
            {
                return String.format("%s @ %s (%s)", processorPath, appId, docName.toString());
            }
            else
            {
                return String.format("%s @ %s", processorPath, appId);
            }
        }
        else
        {
            return String.format("%s @ %s", processorPath, appId);
        }
    }


}
