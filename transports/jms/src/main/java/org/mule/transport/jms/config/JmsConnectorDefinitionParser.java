/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.transport.jms.JmsConnector;

import javax.jms.Session;

public class JmsConnectorDefinitionParser  extends MuleOrphanDefinitionParser
{

    public JmsConnectorDefinitionParser()
    {
        this(JmsConnector.class);
    }

    public JmsConnectorDefinitionParser(Class clazz)
    {
        super(clazz, true);
        addMapping("acknowledgementMode",
            "AUTO_ACKNOWLEDGE=" + Session.AUTO_ACKNOWLEDGE + "," +
            "CLIENT_ACKNOWLEDGE=" + Session.CLIENT_ACKNOWLEDGE + "," +
            "DUPS_OK_ACKNOWLEDGE=" + Session.DUPS_OK_ACKNOWLEDGE);
    }
    
}
