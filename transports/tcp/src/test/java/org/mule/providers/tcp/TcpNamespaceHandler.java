/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.tcp;

import org.w3c.dom.Element;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;

/**
 * todo document
 *
 */
public class TcpNamespaceHandler extends NamespaceHandlerSupport
{


    public void init()
    {
        registerBeanDefinitionParser("connector", new ConnectorDefinitionParser());
    }


    public static class ConnectorDefinitionParser extends AbstractSingleBeanDefinitionParser
    {
        protected Class getBeanClass(Element element) {
            return TcpConnector.class;
        }
    }
}