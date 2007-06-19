/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.MuleProperties;
import org.mule.config.PoolingProfile;

import org.w3c.dom.Element;

/**
 * This parser is responsible for processing the <code><pooling-profile><code> configuration elements.
 */
public class PoolingProfileDefinitionParser extends AbstractChildBeanDefinitionParser
{

    public PoolingProfileDefinitionParser()
    {
        withAlias("initialisationPolicy", "initialisationPolicyString");
        withAlias("exhaustedAction", "exhaustedActionString");
    }

    protected Class getBeanClass(Element element)
    {
        return PoolingProfile.class;
    }

    protected String getParentBeanName(Element element)
    {
        //The mule:configuration element is a fixed name element so we need to handle the
        //special case here
        if("configuration".equals(element.getParentNode().getLocalName()))
        {
            return MuleProperties.OBJECT_MULE_CONFIGURATION;
        }
        else
        {
            return super.getParentBeanName(element);            
        }
    }

    public String getPropertyName(Element e)
    {
        return "poolingProfile";
    }
}
