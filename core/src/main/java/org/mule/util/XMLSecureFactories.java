/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static javax.xml.stream.XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provide XML parser factories configured to avoid XXE and BL attacks according to global configuration (safe by default)
 */
public class XMLSecureFactories
{
    protected final Log logger = LogFactory.getLog(this.getClass());

    public static final String EXTERNAL_ENTITIES_PROPERTY =
            SYSTEM_PROPERTY_PREFIX + "xml.expandExternalEntities";
    public static final String EXPAND_ENTITIES_PROPERTY =
            SYSTEM_PROPERTY_PREFIX + "xml.expandInternalEntities";

    private Boolean externalEntities;
    private Boolean expandEntities;

    public static XMLSecureFactories createWithConfig(Boolean externalEntities, Boolean expandEntities)
    {
        XMLSecureFactories factory = new XMLSecureFactories();

        if (externalEntities != null)
        {
            factory.externalEntities = externalEntities;
        }
        if (expandEntities != null)
        {
            factory.expandEntities = expandEntities;
        }

        return factory;
    }

    public XMLSecureFactories()
    {
        String externalEntitiesValue = System.getProperty(EXTERNAL_ENTITIES_PROPERTY, "false");
        externalEntities = Boolean.parseBoolean(externalEntitiesValue);

        String expandEntitiesValue = System.getProperty(EXPAND_ENTITIES_PROPERTY, "false");
        expandEntities = Boolean.parseBoolean(expandEntitiesValue);
    }

    public DocumentBuilderFactory createDocumentBuilderFactory()
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

    public SAXParserFactory createSaxParserFactory()
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

    public XMLInputFactory createXmlInputFactory()
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        factory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, externalEntities);
        factory.setProperty(IS_REPLACING_ENTITY_REFERENCES, expandEntities);

        return factory;
    }
}
