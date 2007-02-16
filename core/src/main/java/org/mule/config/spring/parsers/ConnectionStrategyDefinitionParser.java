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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.util.ClassUtils;

import org.springframework.beans.FatalBeanException;
import org.w3c.dom.Element;

/**
 * TODO
 */
public class ConnectionStrategyDefinitionParser extends AbstractChildBeanDefinitionParser
{

    public static final Class DEFAULT_CONNECTION_STRATEGY = SimpleRetryConnectionStrategy.class;

    protected Class getBeanClass(Element element)
    {
        //generateBeanNameIfNotSet(element, JdmkAgent.class);
        String clazz = element.getAttribute("class");
        if (clazz != null)
        {
            try
            {
                return ClassUtils.loadClass(clazz, getClass());
            }
            catch (ClassNotFoundException e)
            {
                throw new FatalBeanException(new Message(Messages.CLASS_X_NOT_FOUND, clazz).getMessage(), e);
            }
        }
        return ConnectionStrategyDefinitionParser.DEFAULT_CONNECTION_STRATEGY;
    }

    public String getPropertyName(Element e)
    {
        String name = e.getLocalName();
        if ("receiver-connection-strategy".equals(name))
        {
            return "receiverConnectionStrategy";
        }
        else if ("dispatcher-connection-strategy".equals(name))
        {
            return "dispatcherConnectionStrategy";
        }
        else if (name.startsWith("default-"))
        {
            //If this is one of the default strategy they should just be made available in the contianer
            // and retrieved via the Registry
            return null;
        }
        else
        {
            return "connectionStrategy";
        }
    }
}
