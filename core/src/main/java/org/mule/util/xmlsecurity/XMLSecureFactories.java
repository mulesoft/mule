/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.xmlsecurity;

import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;

/**
 * Provide XML parser factories configured to avoid XXE and BL attacks according to global configuration (safe by default)
 */
public class XMLSecureFactories
{
    public static final String EXTERNAL_ENTITIES_PROPERTY =
            SYSTEM_PROPERTY_PREFIX + "xml.expandExternalEntities";
    public static final String EXPAND_ENTITIES_PROPERTY =
            SYSTEM_PROPERTY_PREFIX + "xml.expandInternalEntities";

    private Boolean externalEntities;
    private Boolean expandEntities;

    public static XMLSecureFactories createWithConfig(Boolean externalEntities, Boolean expandEntities)
    {
        XMLSecureFactories factory = new XMLSecureFactories();

        factory.externalEntities = externalEntities;
        factory.expandEntities = expandEntities;

        return factory;
    }

    public static XMLSecureFactories createDefault()
    {
        return new XMLSecureFactories();
    }


    private XMLSecureFactories()
    {
        String externalEntitiesValue = System.getProperty(EXTERNAL_ENTITIES_PROPERTY, "false");
        externalEntities = Boolean.parseBoolean(externalEntitiesValue);

        String expandEntitiesValue = System.getProperty(EXPAND_ENTITIES_PROPERTY, "false");
        expandEntities = Boolean.parseBoolean(expandEntitiesValue);
    }

    public DocumentBuilderFactory getDocumentBuilderFactory()
    {
        return XMLSecureFactoriesCache.getInstance().getDocumentBuilderFactory(externalEntities, expandEntities);
    }

    public SAXParserFactory getSAXParserFactory()
    {
        return XMLSecureFactoriesCache.getInstance().getSAXParserFactory(externalEntities, expandEntities);
    }

    public XMLInputFactory getXMLInputFactory()
    {
        return XMLSecureFactoriesCache.getInstance().getXMLInputFactory(externalEntities, expandEntities);
    }
}
