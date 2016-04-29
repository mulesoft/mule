/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.config;

import org.mule.runtime.config.spring.parsers.delegate.AbstractFirstResultSerialDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ObjectFactoryWrapper;

/**
 * We want to set the connection factory as a pojo factory and then add attributes (username and
 * password) on the parent
 */
public class ConnectionFactoryDefinitionParser extends AbstractFirstResultSerialDefinitionParser
{
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public ConnectionFactoryDefinitionParser()
    {
        addDelegate(new ObjectFactoryWrapper("connectionFactory")).addIgnored(USERNAME).addIgnored(PASSWORD);
        addDelegate(new ParentDefinitionParser()).setIgnoredDefault(true).removeIgnored(USERNAME).removeIgnored(PASSWORD);
    }
}
