/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.util.SystemUtils;

import org.xml.sax.Attributes;

/**
 * Stores the metadata annotations from the XML parser so they are available when building the actual objects of the
 * application.
 */
public class XmlMetadataAnnotations
{
    public static final String METADATA_ANNOTATIONS_KEY = "metadataAnnotations";
    
    private StringBuilder xmlContent = new StringBuilder();
    private int lineNumber;

    public XmlMetadataAnnotations(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    /**
     * Builds the opening tag of the xml element.
     * 
     * @param qName the qualified name of the element
     * @param atts the attributes of the element
     */
    public void appendElementStart(String qName, Attributes atts)
    {
        xmlContent.append("<" + qName);
        for (int i = 0; i < atts.getLength(); ++i)
        {
            xmlContent.append(" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"");
        }
        xmlContent.append(">");
    }

    /**
     * Adds the body of the xml tag.
     * 
     * @param elementBody the body content to be added
     */
    public void appendElementBody(String elementBody)
    {
        xmlContent.append(elementBody);
    }

    /**
     * Builds the closing tag of the xml element.
     * 
     * @param qName the qualified name of the element
     */
    public void appendElementEnd(String qName)
    {
        xmlContent.append("</" + qName + ">");
    }

    /**
     * @return the reconstruction of the declaration of the element in its source xml file.
     *         <p/>
     *         Note that the order of the elements may be different, and any implicit attributes with default values
     *         will be included.
     */
    public String getElementString()
    {
        return xmlContent.toString()
                         .replaceAll(">\\s+<+", ">" + SystemUtils.LINE_SEPARATOR + "<") /* compact whitespaces and line breaks */
                         .trim();
    }

    /**
     * @return the line where the declaration of the element starts in its source xml file.
     */
    public int getLineNumber()
    {
        return lineNumber;
    }
}