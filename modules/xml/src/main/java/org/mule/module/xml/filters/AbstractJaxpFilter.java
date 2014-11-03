/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.types.DataTypeFactory;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Common filter functionality for filters which need to convert payloads to {@link Document}s.
 */
public abstract class AbstractJaxpFilter
{

    private XmlToDomDocument xmlToDom = new XmlToDomDocument();

    private DocumentBuilderFactory documentBuilderFactory;
    
    public AbstractJaxpFilter()
    {
        super();
        xmlToDom.setReturnDataType(DataTypeFactory.create(Document.class));
    }
    public void initialise() throws InitialisationException
    {
        if (getDocumentBuilderFactory() == null)
        {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            setDocumentBuilderFactory(builderFactory);
        }
    }

    /**
     * @deprecated use {@link #toDOMNode(Object, MuleEvent)} instead
     */
    @Deprecated
    public Node toDOMNode(Object src) throws Exception
    {
        return toDOMNode(src, RequestContext.getEvent());
    }

    public Node toDOMNode(Object src, MuleEvent event) throws Exception
    {
        Node node = XMLUtils.toDOMNode(src, event, getDocumentBuilderFactory());
        return node == null ? (Node) xmlToDom.transform(src) : node;
    }

    /**
     * The document builder factory to use in case XML needs to be parsed.
     * 
     * @return The document builder factory to use in case XML needs to be parsed.
     */
    public DocumentBuilderFactory getDocumentBuilderFactory()
    {
        return documentBuilderFactory;
    }

    /**
     * The document builder factory to use in case XML needs to be parsed.
     * 
     * @param documentBuilderFactory The document builder factory to use in case XML
     *            needs to be parsed.
     */
    public void setDocumentBuilderFactory(DocumentBuilderFactory documentBuilderFactory)
    {
        this.documentBuilderFactory = documentBuilderFactory;
    }
}
