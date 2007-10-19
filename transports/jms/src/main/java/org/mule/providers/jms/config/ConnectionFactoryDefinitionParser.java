/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.config;

import org.mule.config.spring.parsers.delegate.AbstractFirstResultSerialDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

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
        addDelegate(new ObjectFactoryDefinitionParser("connectionFactory").addIgnored(USERNAME).addIgnored(PASSWORD));
        addDelegate(new ParentDefinitionParser().setIgnoredDefault(true).removeIgnored(USERNAME).removeIgnored(PASSWORD));
    }

}
