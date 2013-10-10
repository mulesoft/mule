/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.routing;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.routing.CorrelationMode;
import org.mule.routing.outbound.AbstractRoundRobinMessageSplitter;
import org.mule.util.ExceptionUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.io.StringReader;
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
import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;

/**
 * <code>XmlMessageSplitter</code> will split a DOM4J document into nodes
 * based on the "splitExpression" property. <p/> Optionally, you can specify a
 * <code>namespaces</code> property map that contain prefix/namespace mappings.
 * Mind if you have a default namespace declared you should map it to some namespace,
 * and reference it in the <code>splitExpression</code> property. <p/> The splitter
 * can optionally validate against an XML schema. By default schema validation is
 * turned off. <p/> You may reference an external schema from the classpath by using
 * the <code>externalSchemaLocation</code> property. <p/> Note that each part
 * returned is actually returned as a new Document.
 */
public class XmlMessageSplitter extends AbstractRoundRobinMessageSplitter
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

    protected volatile String splitExpression = "";
    protected volatile Map<String, String> namespaces;
    protected NamespaceManager namespaceManager;

    protected volatile boolean validateSchema;
    protected volatile String externalSchemaLocation = "";

    public void setSplitExpression(String splitExpression)
    {
        this.splitExpression = StringUtils.trimToEmpty(splitExpression);
    }

    public void setNamespaces(Map<String, String> namespaces)
    {
        this.namespaces = namespaces;
    }

    public Map<String, String> getNamespaces()
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

    @Override
    public void initialise() throws InitialisationException
    {
        logger.warn("Deprecation warning: The XmlMessageSplitter router has been deprecating in Mule 2.2 in favour of using the <expression-splitter> router.");
        if (StringUtils.isBlank(splitExpression))
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("splitExpression").getMessage());
        }

        try
        {
            namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);

            if (namespaceManager != null)
            {
                if (namespaces == null)
                {
                    namespaces = new HashMap<String, String>(namespaceManager.getNamespaces());
                }
                else
                {
                    namespaces.putAll(namespaceManager.getNamespaces());
                }
            }

        }
        catch (RegistrationException e)
        {
            throw new InitialisationException(CoreMessages.failedToLoad("NamespaceManager"), e, this);
        }

        super.initialise();
    }

    /**
     * Template method can be used to split the message up before the getMessagePart
     * method is called .
     *
     * @param message the message being routed
     */
    @Override
    protected List splitMessage(MuleMessage message)
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
                src = new String((byte[]) src);
            }

            Document dom4jDoc;

            if (src instanceof String)
            {
                String xml = (String) src;
                SAXReader reader = new SAXReader();
                setDoSchemaValidation(reader, isValidateSchema());

                dom4jDoc = reader.read(new StringReader(xml));
            }
            else if (src instanceof org.dom4j.Document)
            {
                dom4jDoc = (org.dom4j.Document) src;
            }
            else if (src instanceof org.w3c.dom.Document)
            {
                DOMReader xmlReader = new DOMReader();
                dom4jDoc = xmlReader.read((org.w3c.dom.Document)src);
            }
            else
            {
                throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(
                        src.getClass(), new Class[]{org.w3c.dom.Document.class, Document.class, String.class, byte[].class}).getMessage());
            }

            XPath xpath = dom4jDoc.createXPath(splitExpression);
            if (namespaces != null)
            {
                xpath.setNamespaceURIs(namespaces);
            }

            List foundNodes = xpath.selectNodes(dom4jDoc);
            if (enableCorrelation != CorrelationMode.NEVER)
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
                Node node = (Node) iterator.next();
                if (node instanceof Element)
                {
                    // Can't do detach here just in case the source object
                    // was a document.
                    node = (Node) node.clone();
                    parts.add(DocumentHelper.createDocument((Element) node));
                }
                else
                {
                    logger.warn("Dcoument node: " + node.asXML()
                            + " is not an element and thus is not a valid part");
                }
            }
            return parts;
        }
        catch (Exception ex)
        {
            throw new IllegalArgumentException("Failed to initialise the payload: "
                    + ExceptionUtils.getStackTrace(ex));
        }
    }

    protected void setDoSchemaValidation(SAXReader reader, boolean validate) throws Exception
    {
        reader.setValidation(validate);
        reader.setFeature(APACHE_XML_FEATURES_VALIDATION_SCHEMA, validate);
        reader.setFeature(APACHE_XML_FEATURES_VALIDATION_SCHEMA_FULL_CHECKING, true);

        // By default we're not validating against an XSD. If this is the case,
        // there's no need to continue here, so we bail.
        if (!validate)
        {
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
