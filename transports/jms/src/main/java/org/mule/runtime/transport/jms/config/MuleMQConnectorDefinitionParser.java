/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.config;


public class MuleMQConnectorDefinitionParser extends JmsConnectorDefinitionParser
{
    public MuleMQConnectorDefinitionParser()
    {
        super();
        addAlias("brokerURL", "realmURL");
    }

    public MuleMQConnectorDefinitionParser(Class clazz)
    {
        super(clazz);
        addAlias("brokerURL", "realmURL");
    }
}
