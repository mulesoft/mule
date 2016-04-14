/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.execution;

import org.mule.api.AnnotatedObject;

import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Provides a standard way to generate a log entry or message that references an element in a flow.
 * 
 * @since 3.8.0
 */
public abstract class LocationExecutionContextProvider implements ExceptionContextProvider
{

    private static final QName NAME_ANNOTATION_KEY = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");
    private static final QName SOURCE_FILE_ANNOTATION_KEY = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName");
    private static final QName SOURCE_FILE_LINE_ANNOTATION_KEY = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");
    private static final QName SOURCE_ELEMENT_ANNOTATION_KEY = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceElement");

    /**
     * Populates the passed beanAnnotations with the other passed parameters.
     * 
     * @param beanAnnotations the map with annotations to populate
     * @param fileName the name of the file where the element definition was read from.
     * @param lineNumber the line number where the definition of the element starts in the file.
     * @param xmlContent the xml representation of the element definition.
     */
    public static void addMetadataAnnotationsFromXml(Map<QName, Object> beanAnnotations, String fileName, int lineNumber, String xmlContent)
    {
        beanAnnotations.put(SOURCE_FILE_ANNOTATION_KEY, fileName);
        beanAnnotations.put(SOURCE_FILE_LINE_ANNOTATION_KEY, lineNumber);
        beanAnnotations.put(SOURCE_ELEMENT_ANNOTATION_KEY, xmlContent);
    }

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
        String docName = getDocName(element);
        if (docName != null)
        {
            return String.format("%s @ %s:%s:%d (%s)", processorPath, appId, getSourceFile((AnnotatedObject) element), getSourceFileLine((AnnotatedObject) element), docName);
        }
        else
        {
            if (element instanceof AnnotatedObject)
            {
                return String.format("%s @ %s:%s:%d", processorPath, appId, getSourceFile((AnnotatedObject) element), getSourceFileLine((AnnotatedObject) element));
            }
            else
            {
                return String.format("%s @ %s", processorPath, appId);
            }
        }
    }

    /**
     * @param element the element to get the {@code doc:name} from.
     * @return the {@code doc:name} attribute value of the element.
     */
    public static String getDocName(Object element)
    {
        if (element instanceof AnnotatedObject)
        {
            Object docName = ((AnnotatedObject) element).getAnnotation(NAME_ANNOTATION_KEY);
            return docName != null ? docName.toString() : null;
        }
        else
        {
            return null;
        }
    }

    protected static String getSourceFile(AnnotatedObject element)
    {
        return (String) element.getAnnotation(SOURCE_FILE_ANNOTATION_KEY);
    }

    protected static Integer getSourceFileLine(AnnotatedObject element)
    {
        return (Integer) element.getAnnotation(SOURCE_FILE_LINE_ANNOTATION_KEY);
    }

    protected static String getSourceXML(AnnotatedObject element)
    {
        Object sourceXml = element.getAnnotation(SOURCE_ELEMENT_ANNOTATION_KEY);
        return sourceXml != null ? sourceXml.toString() : null;
    }

}
