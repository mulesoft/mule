/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.config;

import org.mule.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
import org.mule.module.client.remoting.RemoteDispatcherAgent;
@Deprecated
public class RemoteDispatcherAgentDefinitionParser extends DefaultNameMuleOrphanDefinitionParser
{
    public RemoteDispatcherAgentDefinitionParser()
    {
        super(RemoteDispatcherAgent.class);
    }
}
