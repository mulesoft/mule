/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.providers.ConnectionStrategy;
import org.mule.providers.SimpleRetryConnectionStrategy;

/**
 * Handles the parsing of <code><mule:connection-strategy>, <mule:dispatcher-connection-strategy>,
 * <mule:receiver-connection-strategy></code> elements in Mule Xml configuration.
 */
public class ConnectionStrategyDefinitionParser extends ChildDefinitionParser
{

    public static final Class DEFAULT_CONNECTION_STRATEGY = SimpleRetryConnectionStrategy.class;

    public ConnectionStrategyDefinitionParser(String propertyName)
    {
        super(propertyName, DEFAULT_CONNECTION_STRATEGY, ConnectionStrategy.class, true);
    }

    /**
     * Default connection strategies are available in the registry, but have n0 parent
     */
    public ConnectionStrategyDefinitionParser()
    {
        super(null, DEFAULT_CONNECTION_STRATEGY, ConnectionStrategy.class, true);
    }

}
