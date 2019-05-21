/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.xmlsecurity;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.util.xmlsecurity.XMLSecureFactoriesCache.getInstance;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

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

    public static XMLSecureFactories createDefault()
    {
        return new XMLSecureFactories();
    }


    private XMLSecureFactories()
    {
        String externalEntitiesValue = getProperty(EXTERNAL_ENTITIES_PROPERTY, "false");
        externalEntities = parseBoolean(externalEntitiesValue);

        String expandEntitiesValue = getProperty(EXPAND_ENTITIES_PROPERTY, "false");
        expandEntities = parseBoolean(expandEntitiesValue);
    }

    public DocumentBuilderFactory getDocumentBuilderFactory()
    {
        return getInstance().getDocumentBuilderFactory(externalEntities, expandEntities);
    }

    public SAXParserFactory getSAXParserFactory()
    {
        return getInstance().getSAXParserFactory(externalEntities, expandEntities);
    }

    public XMLInputFactory getXMLInputFactory()
    {
        return getInstance().getXMLInputFactory(externalEntities, expandEntities);
    }

    public TransformerFactory getTransformerFactory()
    {
        return getInstance().getTransformerFactory(externalEntities, expandEntities);
    }

    public void configureTransformerFactory(TransformerFactory factory)
    {
        DefaultXMLSecureFactories.configureTransformerFactory(externalEntities, expandEntities, factory);
    }

    public void configureSchemaFactory(SchemaFactory factory)
    {
        DefaultXMLSecureFactories.configureSchemaFactory(externalEntities, expandEntities, factory);
    }

    public void configureValidator(Validator validator)
    {
        DefaultXMLSecureFactories.configureValidator(externalEntities, expandEntities, validator);
    }
}
