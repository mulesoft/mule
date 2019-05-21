/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.xmlsecurity;

import static java.lang.String.format;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static javax.xml.stream.XMLInputFactory.newInstance;
import static org.apache.commons.logging.LogFactory.getLog;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;

/**
 * Create different XML factories configured through the same interface for disabling vulnerabilities.
 */
public class DefaultXMLSecureFactories
{
    private final static Log logger = getLog(DefaultXMLSecureFactories.class);

    public static DocumentBuilderFactory createDocumentBuilderFactory(Boolean externalEntities, Boolean expandEntities)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try
        {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", externalEntities);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", externalEntities);
            factory.setExpandEntityReferences(expandEntities);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !expandEntities);
        }
        catch (Exception e)
        {
            logWarning("DocumentBuilderFactory", factory.getClass().getName());
        }

        return factory;
    }

    public static SAXParserFactory createSaxParserFactory(Boolean externalEntities, Boolean expandEntities)
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try
        {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", externalEntities);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", externalEntities);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !expandEntities);
        }
        catch (Exception e)
        {
            logWarning("SAXParserFactory", factory.getClass().getName());
        }

        return factory;
    }

    public static XMLInputFactory createXmlInputFactory(Boolean externalEntities, Boolean expandEntities)
    {
        XMLInputFactory factory = newInstance();

        factory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, externalEntities);
        factory.setProperty(SUPPORT_DTD, expandEntities);
        return factory;
    }

    public static TransformerFactory createTransformerFactory(Boolean externalEntities, Boolean expandEntities)
    {
        TransformerFactory factory = TransformerFactory.newInstance();

        configureTransformerFactory(externalEntities, expandEntities, factory);

        return factory;
    }

  /**
   * Disables {@link XMLConstants#ACCESS_EXTERNAL_DTD} and {@link XMLConstants#ACCESS_EXTERNAL_STYLESHEET} features.
   *
   * @see TransformerFactory#setAttribute(String, Object) for more information about supported attributes.
   *
   * @param factory the {@link TransformerFactory} to configure.
   */
    public static void configureTransformerFactory(Boolean externalEntities, Boolean expandEntities, TransformerFactory factory)
    {
        if (!externalEntities && !expandEntities)
        {
            try
            {
                factory.setAttribute(ACCESS_EXTERNAL_STYLESHEET, "");
                factory.setAttribute(ACCESS_EXTERNAL_DTD, "");
            }
            catch (Exception e)
            {
                logWarning("TransformerFactory", factory.getClass().getName());
            }
        }
    }

    /**
     * Disables {@link XMLConstants#ACCESS_EXTERNAL_DTD} and {@link XMLConstants#ACCESS_EXTERNAL_DTD} features
     *
     * @see SchemaFactory#setProperty(String, Object) for more information about supported properties.
     *
     * @param factory the {@link SchemaFactory} to configure.
     */
    public static void configureSchemaFactory(Boolean externalEntities, Boolean expandEntities, SchemaFactory factory)
    {
        if (!externalEntities && !expandEntities)
        {
            try
            {
                factory.setProperty(ACCESS_EXTERNAL_SCHEMA, "");
                factory.setProperty(ACCESS_EXTERNAL_DTD, "");
            }
            catch (Exception e)
            {
                logWarning("SchemaFactory", factory.getClass().getName());
            }
        }
    }

    /**
     * Disables {@link XMLConstants#ACCESS_EXTERNAL_DTD} and {@link XMLConstants#ACCESS_EXTERNAL_DTD} features
     *
     * @see Validator#setProperty(String, Object) for more information about supported properties.
     *
     * @param validator the {@link Validator} to configure.
     */
    public static void configureValidator(Boolean externalEntities, Boolean expandEntities, Validator validator)
    {
        if (!externalEntities && !expandEntities)
        {
            try
            {
                validator.setProperty(ACCESS_EXTERNAL_SCHEMA, "");
                validator.setProperty(ACCESS_EXTERNAL_DTD, "");
            }
            catch (Exception e)
            {
                logWarning("Validator", validator.getClass().getName());
            }
        }
    }

    protected static void logWarning(String interfaceName, String implementationName)
    {
        logger.warn(format("Can't configure XML entity expansion for %s (%s), this could introduce XXE and BL vulnerabilities", interfaceName, implementationName));
    }
}
