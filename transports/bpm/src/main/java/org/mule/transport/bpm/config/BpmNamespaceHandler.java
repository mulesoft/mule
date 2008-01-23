/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.bpm.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.bpm.ProcessConnector;

/**
 * Registers a Bean Definition Parsers for the "bpm" namespace.
 */
public class BpmNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String PROCESS = "process";

    public void init()
    {
        registerStandardTransportEndpoints(ProcessConnector.BPM, new String[]{PROCESS}).addAlias(PROCESS, URIBuilder.PATH);
        registerConnectorDefinitionParser(ProcessConnector.class);
    }

}