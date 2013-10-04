/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.w3c.dom.Element;

/**
 * Extend {@link org.mule.config.spring.parsers.generic.ChildDefinitionParser} to include
 * logic for identifying parent configuration element (since this only applies to "default"
 * elements there's an ugliness here - contradicitions (non-default children of configuration)
 * are avoided by the mule.xsd schema).
 */
public class ConfigurationChildDefinitionParser extends ChildDefinitionParser
{

    /** Name of the mule:configuration element **/
    public static final String CONFIGURATION = "configuration";

    public ConfigurationChildDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
    }

    protected String getParentBeanName(Element element)
    {
        //The mule:configuration element is a fixed name element so we need to handle the
        //special case here
        if (CONFIGURATION.equals(element.getParentNode().getLocalName()))
        {
            return MuleProperties.OBJECT_MULE_CONFIGURATION;
        }
        else
        {
            return super.getParentBeanName(element);
        }
    }

}
