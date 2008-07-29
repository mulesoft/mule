/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.xml.util.XMLUtils;
import org.mule.routing.outbound.AbstractMessageSplitter;
import org.mule.util.ExceptionUtils;
import org.mule.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;

/**
 * <code>FilteringXmlMessageSplitter</code> will split a DOM4J document into nodes
 * based on the "splitExpression" property. <p/> Optionally, you can specify a
 * <code>namespaces</code> property map that contain prefix/namespace mappings.
 * Mind if you have a default namespace declared you should map it to some namespace,
 * and reference it in the <code>splitExpression</code> property. <p/> The splitter
 * can optionally validate against an XML schema. By default schema validation is
 * turned off. <p/> You may reference an external schema from the classpath by using
 * the <code>externalSchemaLocation</code> property. <p/> Note that each part
 * returned is actually returned as a new Document.
 */
public class FilteringXmlMessageSplitter extends AbstractMessageSplitter
{
    protected final ThreadLocal propertiesContext = new ThreadLocal();
    protected final ThreadLocal nodesContext = new ThreadLocal();

    protected volatile String splitExpression = "";
    protected volatile Map namespaces = null;
    protected volatile boolean validateSchema = false;
    protected volatile String externalSchemaLocation = "";

    public void setSplitExpression(String splitExpression)
    {
        this.splitExpression = StringUtils.trimToEmpty(splitExpression);
    }

    public void setNamespaces(Map namespaces)
    {
        this.namespaces = namespaces;
    }

    public Map getNamespaces()
    {
        return Collections.unmodifiableMap(namespaces);
    }

    public String getSplitExpression()
    {
        return splitExpression;
    }

    public boolean isValidateSchema()
    {
        return validateSchema;
    }

    public void setValidateSchema(boolean validateSchema)
    {
        this.validateSchema = validateSchema;
    }

    public String getExternalSchemaLocation()
    {
        return externalSchemaLocation;
    }

    /**
     * Set classpath location of the XSD to check against. If the resource cannot be
     * found, an exception will be thrown at runtime.
     * 
     * @param externalSchemaLocation location of XSD
     */
    public void setExternalSchemaLocation(String externalSchemaLocation)
    {
        this.externalSchemaLocation = externalSchemaLocation;
    }

    /**
     * Template method can be used to split the message up before the getMessagePart
     * method is called .
     * 
     * @param message the message being routed
     */
    // @Override
    protected void initialise(MuleMessage message)
    {
        if (logger.isDebugEnabled())
        {
            if (splitExpression.length() == 0)
            {
                logger.warn("splitExpression is not specified, no processing will take place");
            }
            else
            {
                logger.debug("splitExpression is " + splitExpression);
            }
        }

        Object src = message.getPayload();

        Document dom4jDoc;
        try
        {
            if (validateSchema)
            {
                dom4jDoc = XMLUtils.toDocument(src, getExternalSchemaLocation());
            }
            else
            {
                dom4jDoc = XMLUtils.toDocument(src);
            }
            if (dom4jDoc == null)
            {
                logger.error("Non-XML message payload: " + src.getClass().toString());
                return;
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Failed to initialise the payload: "
                + ExceptionUtils.getStackTrace(e));
        }

        if (splitExpression.length() > 0)
        {
            XPath xpath = dom4jDoc.createXPath(splitExpression);
            if (namespaces != null)
            {
                xpath.setNamespaceURIs(namespaces);
            }

            List foundNodes = xpath.selectNodes(dom4jDoc);
            if (enableCorrelation != ENABLE_CORRELATION_NEVER)
            {
                message.setCorrelationGroupSize(foundNodes.size());
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Split into " + foundNodes.size());
            }

            List parts = new LinkedList();
            // Rather than reparsing these when individual messages are
            // created, lets do it now
            // We can also avoid parsing the Xml again altogether
            for (Iterator iterator = foundNodes.iterator(); iterator.hasNext();)
            {
                Node node = (Node)iterator.next();
                if (node instanceof Element)
                {
                    // Can't do detach here just in case the source object
                    // was a document.
                    node = (Node)node.clone();
                    parts.add(DocumentHelper.createDocument((Element)node));
                }
                else
                {
                    logger.warn("Dcoument node: " + node.asXML()
                                + " is not an element and thus is not a valid part");
                }
            }
            nodesContext.set(parts);
        }

        Map theProperties = new HashMap();
        for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String)iterator.next();
            theProperties.put(propertyKey, message.getProperty(propertyKey));
        }
        propertiesContext.set(theProperties);
    }

    // @Override
    protected void cleanup()
    {
        nodesContext.set(null);
        propertiesContext.set(null);
    }
    
    /**
     * Retrieves a specific message part for the given endpoint. the message will
     * then be routed via the provider.
     * 
     * @param message the current message being processed
     * @param endpoint the endpoint that will be used to route the resulting message
     *            part
     * @return the message part to dispatch
     */
    protected MuleMessage getMessagePart(MuleMessage message, OutboundEndpoint endpoint)
    {
        List nodes = (List)nodesContext.get();

        if (nodes == null)
        {
            logger.error("Error: nodes are null");
            return null;
        }

        for (Iterator i = nodes.iterator(); i.hasNext();)
        {
            Document doc = (Document)i.next();

            try
            {
                Map theProperties = (Map)propertiesContext.get();
                MuleMessage result = new DefaultMuleMessage(doc, new HashMap(theProperties));

                if (endpoint.getFilter() == null || endpoint.getFilter().accept(result))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Endpoint filter matched for node " + i + " of " + nodes.size()
                                     + ". Routing message over: " + endpoint.getEndpointURI().toString());
                    }
                    i.remove();
                    return result;
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Endpoint filter did not match, returning null");
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Unable to create message for node at position " + i, e);
                return null;
            }
        }

        return null;
    }
}
