/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.spring.parsers.AbstractChildDefinitionParser;

import org.w3c.dom.Element;

/**
 * This parser is responsible for processing the <code><threading-profile><code> configuration elements.
 */
public class ThreadingProfileDefinitionParser extends AbstractChildDefinitionParser
{

    public ThreadingProfileDefinitionParser()
    {
        addAlias("poolExhaustedAction", "poolExhaustedActionString");
    }

    protected Class getBeanClass(Element element)
    {
        return ThreadingProfile.class;
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
        String name = e.getLocalName();
        if ("receiver-threading-profile".equals(name))
        {
            return "receiverThreadingProfile";
        }
        else if ("dispatcher-threading-profile".equals(name))
        {
            return "dispatcherThreadingProfile";
        }
        else if (name.equals("default-threading-profile"))
        {
            //If this is one of the default profiles they should just be made available in the contianer
            // and retrieved via the Registry
            return "defaultThreadingProfile";
        }
        else if ("default-receiver-threading-profile".equals(name))
        {
            return "defaultMessageReceiverThreadingProfile";
        }
        else if ("default-dispatcher-threading-profile".equals(name))
        {
            return "defaultMessageDispatcherThreadingProfile";
        }
        else if ("default-threading-profile".equals(name))
        {
            //If this is one of the default profiles they should just be made available in the contianer
            // and retrieved via the Registry
            return "defaultThreadingProfile";
        }
        else
        {
            return "threadingProfile";
        }
    }
}
