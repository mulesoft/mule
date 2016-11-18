/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.xmlsecurity;

import static javax.xml.stream.XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;

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
            logger.warn("Can't configure XML entity expansion for DocumentBuilderFactory, this could introduce XXE and BL vulnerabilities");
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
            logger.warn("Can't configure XML entity expansion for SAXParserFactory, this could introduce XXE and BL vulnerabilities");
        }

        return factory;
    }

    public static XMLInputFactory createXmlInputFactory(Boolean externalEntities, Boolean expandEntities)
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        factory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, externalEntities);
        factory.setProperty(IS_REPLACING_ENTITY_REFERENCES, expandEntities);

        return factory;
    }
}
