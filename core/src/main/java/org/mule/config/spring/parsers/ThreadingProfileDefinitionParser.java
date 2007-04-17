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

import org.mule.config.ThreadingProfile;

import org.w3c.dom.Element;

/**
 * This parser is responsible for processing the <code><threading-profile><code> configuration elements.
 */
public class ThreadingProfileDefinitionParser extends AbstractChildBeanDefinitionParser
{

    public ThreadingProfileDefinitionParser()
    {
        registerAttributeMapping("poolExhaustedAction", "poolExhaustedActionString");
    }

    protected Class getBeanClass(Element element)
    {
        return ThreadingProfile.class;
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
        else if (name.startsWith("default-threading-profile"))
        {
            //If this is one of the default profiles they should just be made available in the contianer
            // and retrieved via the Registry
            return "defaultThreadingProfile";
        }
        else if ("default-receiver-threading-profile".equals(name))
        {
            return "defaultReceiverThreadingProfile";
        }
        else if ("default-dispatcher-threading-profile".equals(name))
        {
            return "defaultDispatcherThreadingProfile";
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
