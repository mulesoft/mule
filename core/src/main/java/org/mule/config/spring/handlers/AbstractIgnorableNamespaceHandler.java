/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This Namespace handler extends the default Spring {@link org.springframework.beans.factory.xml.NamespaceHandlerSupport}
 * to allow certain elements in document to be ignored by the handler.
 */
public abstract class AbstractIgnorableNamespaceHandler extends NamespaceHandlerSupport
{
    protected final void registerIgnoredElement(String name)
    {
        registerBeanDefinitionParser(name, new IgnoredDefinitionParser());
    }

    private class IgnoredDefinitionParser implements BeanDefinitionParser
    {

        public BeanDefinition parse(Element element, ParserContext parserContext)
        {
            /*
               This MUST return null, otherwise duplicate BeanDefinitions will be registered,
               which is wrong. E.g. for this config snippet we want only 1 SSL connector
               created, not 4!

                   <ssl:connector name="SslConnector">
                       <ssl:ssl-client
                               clientKeyStore="clientKeyStore"
                               clientKeyStorePassword="mulepassword"/>
                       <ssl:ssl-key-store
                               keyStore="serverKeystore"
                               keyStorePassword="mulepassword"
                               keyPassword="mulepassword"/>
                       <ssl:ssl-server
                               trustStore="trustStore"
                               trustStorePassword="mulepassword"/>
                   </ssl:connector>


            */
            return null;
        }
    }
}