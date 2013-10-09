/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.stdio.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.stdio.PromptStdioConnector;
import org.mule.transport.stdio.StdioConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class StdioNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String SYSTEM_ATTRIBUTE = "system";
    public static final String SYSTEM_MAP =
            "IN=" + StdioConnector.STREAM_SYSTEM_IN +
            ",OUT=" + StdioConnector.STREAM_SYSTEM_OUT +
            ",ERR=" + StdioConnector.STREAM_SYSTEM_ERR;
    public static final String[] SYSTEM_ATTRIBUTE_ARRAY = new String[]{SYSTEM_ATTRIBUTE};

    public void init()
    {
        registerStandardTransportEndpoints(StdioConnector.STDIO, SYSTEM_ATTRIBUTE_ARRAY).addMapping(SYSTEM_ATTRIBUTE, SYSTEM_MAP).addAlias(SYSTEM_ATTRIBUTE, URIBuilder.PATH);
        registerConnectorDefinitionParser(PromptStdioConnector.class);
    }

}
