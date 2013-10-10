/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.config;

import org.mule.config.spring.parsers.delegate.AbstractFirstResultSerialDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryWrapper;

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
