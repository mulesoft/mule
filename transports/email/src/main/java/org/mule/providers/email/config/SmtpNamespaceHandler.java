/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email.config;

import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.providers.email.SmtpConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class SmtpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(SmtpConnector.SMTP, LazyEndpointURI.HOST_ATTRIBUTES);
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(SmtpConnector.class, true));
        registerBeanDefinitionParser("header", new ChildMapEntryDefinitionParser("customHeaders", "key", "value"));
    }
}