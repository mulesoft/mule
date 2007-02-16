/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;

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
    // xml parser feature names for optional XSD validation
    public static final String APACHE_XML_FEATURES_VALIDATION_SCHEMA = "http://apache.org/xml/features/validation/schema";
    public static final String APACHE_XML_FEATURES_VALIDATION_SCHEMA_FULL_CHECKING = "http://apache.org/xml/features/validation/schema-full-checking";

    // JAXP property for specifying external XSD location
    public static final String JAXP_PROPERTIES_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    // JAXP properties for specifying external XSD language (as required by newer
    // JAXP implementation)
    public static final String JAXP_PROPERTIES_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String JAXP_PROPERTIES_SCHEMA_LANGUAGE_VALUE = "http://www.w3.org/2001/XMLSchema";

    protected static final ThreadLocal propertiesContext = new ThreadLocal();
    protected static final ThreadLocal nodesContext = new ThreadLocal();

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
    protected void initialise(UMOMessage message)
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

        try
        {
            if (src instanceof byte[])
            {
                src = new String((byte[])src);
            }

            Document dom4jDoc;

            if (src instanceof String)
            {
                String xml = (String)src;
                SAXReader reader = new SAXReader();
                setDoSchemaValidation(reader, isValidateSchema());
                
                dom4jDoc = reader.read(new StringReader(xml));
            }
            else if (src instanceof org.dom4j.Document)
            {
                dom4jDoc = (org.dom4j.Document)src;
            }
            else
            {
                logger.error("Non-XML message payload: " + src.getClass().toString());
                return;
            }

            if (dom4jDoc != null)
            {
                if (splitExpression.length() > 0)
                {
                    XPath xpath = dom4jDoc.createXPath(splitExpression);
                    if (namespaces != null)
                    {
                        xpath.setNamespaceURIs(namespaces);
                    }

                    List foundNodes = xpath.selectNodes(dom4jDoc);
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
            }
            else
            {
                logger.warn("Unsupported message type, ignoring");
            }
        }
        catch (Exception ex)
        {
            throw new IllegalArgumentException("Failed to initialise the payload: "
                                               + ExceptionUtils.getStackTrace(ex));
        }

        Map theProperties = new HashMap();
        for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String)iterator.next();
            theProperties.put(propertyKey, message.getProperty(propertyKey));
        }
        propertiesContext.set(theProperties);
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
    protected UMOMessage getMessagePart(UMOMessage message, UMOEndpoint endpoint)
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
                UMOMessage result = new MuleMessage(doc, new HashMap(theProperties));

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

    protected void setDoSchemaValidation(SAXReader reader, boolean validate) throws Exception
    {
        reader.setValidation(validate);
        reader.setFeature(APACHE_XML_FEATURES_VALIDATION_SCHEMA, validate);
        reader.setFeature(APACHE_XML_FEATURES_VALIDATION_SCHEMA_FULL_CHECKING, true);
        
        /*
         * By default we're not validating against an XSD. If this is the case,
         * there's no need to continue here, so we bail.
         */
        if (!validate) {
         return;
        }

        InputStream xsdAsStream = IOUtils.getResourceAsStream(getExternalSchemaLocation(), getClass());
        if (xsdAsStream == null)
        {
            throw new IllegalArgumentException("Couldn't find schema at "
                                               + getExternalSchemaLocation());
        }

        // Set schema language property (must be done before the schemaSource
        // is set)
        reader.setProperty(JAXP_PROPERTIES_SCHEMA_LANGUAGE, JAXP_PROPERTIES_SCHEMA_LANGUAGE_VALUE);

        // Need this one to map schemaLocation to a physical location
        reader.setProperty(JAXP_PROPERTIES_SCHEMA_SOURCE, xsdAsStream);
    }
}
