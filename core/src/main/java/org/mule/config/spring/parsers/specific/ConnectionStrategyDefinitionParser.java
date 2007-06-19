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

import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.parsers.AbstractChildDefinitionParser;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.util.ClassUtils;

import org.springframework.beans.FatalBeanException;
import org.w3c.dom.Element;

/**
 * Handles the parsing of <code><mule:connection-strategy>, <mule:dispatcher-connection-strategy>,
 * <mule:receiver-connection-strategy></code> elements in Mule Xml configuration.
 */
public class ConnectionStrategyDefinitionParser extends AbstractChildDefinitionParser
{

    public static final Class DEFAULT_CONNECTION_STRATEGY = SimpleRetryConnectionStrategy.class;

    protected Class getBeanClass(Element element)
    {
        String clazz = element.getAttribute("class");
        if (clazz != null)
        {
            try
            {
                return ClassUtils.loadClass(clazz, getClass());
            }
            catch (ClassNotFoundException e)
            {
                throw new FatalBeanException(CoreMessages.cannotLoadFromClasspath(clazz).getMessage(), e);
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
