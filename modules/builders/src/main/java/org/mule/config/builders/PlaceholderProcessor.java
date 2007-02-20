/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.config.ConfigurationException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.security.PasswordBasedEncryptionStrategy;
import org.mule.util.TemplateParser;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Placeholders are ant-like tags that are embedded in Mule Xml configuration i.e.
 * ${property.name} and are used to swap in property values registered with the Mule
 * container instance when the configuration is loaded. This is a helper class used
 * for parsing these tags.
 */
public class PlaceholderProcessor
{
    public static final String MULE_ENCRYPTION_PROPERTIES = "org.mule.config.encryption.properties";
    public static final String DEFAULT_ENCRYPTION_PROPERTIES_FILE = "mule-encryption.properties";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(PlaceholderProcessor.class);

    private static boolean strategiesLoaded = false;

    private final Map types;
    private final Map schemes = new HashMap();
    private final TemplateParser parser = TemplateParser.createAntStyleParser();

    public PlaceholderProcessor()
    {
        types = new HashMap();
        types.put("PBE", PasswordBasedEncryptionStrategy.class.getName());
    }

    public PlaceholderProcessor(Map types)
    {
        this.types = types;
    }

    public Attributes processAttributes(Attributes attributes, String elementName)
        throws ConfigurationException
    {
        AttributesImpl attribs = new AttributesImpl(attributes);
        String value = null;

        for (int i = 0; i < attribs.getLength(); i++)
        {
            value = attribs.getValue(i);
            value = processValue(value);
            if (value == null)
            {
                throw new ConfigurationException(new Message(Messages.PROPERTY_TEMPLATE_MALFORMED_X,
                    "<" + elementName + attribs.getLocalName(i) + "='" + value + "' ...>"));
            }
            attribs.setValue(i, value);
        }
        return attribs;
    }

    public String processValue(String value) throws ConfigurationException
    {
        //TODO RM*
        return null;
        //return parser.parse(managementContext.getProperties(), value);
    }

    // public String processValue(String value) throws ConfigurationException {
    // String realValue = null;
    // String key = null;
    //
    // UMOManager manager = managementContext;
    //
    // parser.parse(manager.getProperties(), value);
    // int x = value.indexOf("${");
    // while(x > -1) {
    // int y = value.indexOf("}", x +1);
    // if(y==-1) {
    // return null;
    // }
    // key = value.substring(x+2, y);
    // realValue = (String)manager.getProperty(key);
    // if(logger.isDebugEnabled()) {
    // logger.debug("Param is '" + value + "', Property key is '" + key + "',
    // Property value is '" + realValue + "'");
    // }
    // if(realValue!=null) {
    // realValue = processEncryptedValue(realValue);
    // if(realValue==null) {
    // return null;
    // }
    // value = value.substring(0, x) + realValue + value.substring(y+1);
    //
    // // fix for a bug when realValue.length <= key.length
    // y = y - 3 + (realValue.length() - key.length());
    // } else {
    // logger.info("Property for placeholder: '" + key + "' was not found. Leaving
    // place holder as is. This is not necessarily a problem as the placeholder may
    // not be a Mule placeholder.");
    // }
    // x = value.indexOf("${", y);
    // }
    // return value;
    // }


}
