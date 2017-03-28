/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.xmlsecurity;

import static java.lang.String.format;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Create different XML factories configured through the same interface for disabling vulnerabilities.
 */
public class DefaultXMLSecureFactories
{
    private final static Log logger = LogFactory.getLog(DefaultXMLSecureFactories.class);

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
        XMLInputFactory factory = XMLInputFactory.newInstance();

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

    public static void configureTransformerFactory(Boolean externalEntities, Boolean expandEntities, TransformerFactory factory)
    {
        if (!externalEntities && !expandEntities)
        {
            try
            {
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            }
            catch (Exception e)
            {
                logWarning("TransformerFactory", factory.getClass().getName());
            }
        }
    }

    public static void configureSchemaFactory(Boolean externalEntities, Boolean expandEntities, SchemaFactory factory)
    {
        if (!externalEntities && !expandEntities)
        {
            try
            {
                factory.setProperty(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            }
            catch (Exception e)
            {
                logWarning("SchemaFactory", factory.getClass().getName());
            }
        }
    }

    public static void configureValidator(Boolean externalEntities, Boolean expandEntities, Validator validator)
    {
        if (!externalEntities && !expandEntities)
        {
            try
            {
                validator.setProperty(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
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
