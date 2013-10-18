/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import org.mule.RequestContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.OutputHandler;
import org.mule.module.xml.transformer.DelayedResult;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.transformer.types.DataTypeFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.dom.DOMDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

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
    
    public Node toDOMNode(Object src) throws Exception
    {
        if (src instanceof Node)
        {
            return (Document) src;
        }
        else if (src instanceof org.dom4j.Document)
        {
            org.dom4j.Document dom4j = (org.dom4j.Document) src;
            DOMDocument dom = new DOMDocument();
            dom.setDocument(dom4j);
            return dom;
        }
        else if (src instanceof OutputHandler)
        {
            OutputHandler handler = ((OutputHandler) src);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            handler.write(RequestContext.getEvent(), output);
            InputStream stream = new ByteArrayInputStream(output.toByteArray());
            return getDocumentBuilderFactory().newDocumentBuilder().parse(stream);
        }
        else if (src instanceof byte[])
        {
            ByteArrayInputStream stream = new ByteArrayInputStream((byte[]) src);
            return getDocumentBuilderFactory().newDocumentBuilder().parse(stream);
        }
        else if (src instanceof InputStream)
        {
            return getDocumentBuilderFactory().newDocumentBuilder().parse((InputStream) src);
        }
        else if (src instanceof String)
        {
            return getDocumentBuilderFactory().newDocumentBuilder().parse(
                new InputSource(new StringReader((String) src)));
        }
        else if (src instanceof XMLStreamReader)
        {
            XMLStreamReader xsr = (XMLStreamReader) src;
    
            // StaxSource requires that we advance to a start element/document event
            if (!xsr.isStartElement() && xsr.getEventType() != XMLStreamConstants.START_DOCUMENT)
            {
                xsr.nextTag();
            }
    
            return getDocumentBuilderFactory().newDocumentBuilder().parse(new InputSource());
        }
        else if (src instanceof DelayedResult)
        {
            DelayedResult result = ((DelayedResult) src);
            DOMResult domResult = new DOMResult();
            result.write(domResult);
            return domResult.getNode();
        }
        else
        {
            return (Node) xmlToDom.transform(src);
        }
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
