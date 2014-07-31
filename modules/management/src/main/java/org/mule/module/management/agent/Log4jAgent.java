/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Log4jAgent</code> exposes the configuration of the Log4J instance running
 * in Mule for Jmx management
 *
 * @deprecated deprecated since Mule 3.6.0. This will no longer function since log4j2 supports JMX out of the box.
 * Check migration guide for more information
 */
@Deprecated
public class Log4jAgent extends AbstractAgent
{

    private static final Logger logger = LoggerFactory.getLogger(Log4jAgent.class);

    public Log4jAgent()
    {
        super("jmx-log4j");
    }

    @Override
    public String getDescription()
    {
        return "JMX Log4J Agent";
    }

    @Override
    public void initialise() throws InitialisationException
    {
        logger.warn("Log4jAgent is deprecated since Mule 3.6.0. This will no longer function since log4j2 supports JMX out of the box. +" +
                    "Check migration guide for more information");
    }

    @Override
    public void start() throws MuleException
    {
        // nothing to do
    }

    @Override
    public void stop() throws MuleException
    {
        // nothing to do
    }

    @Override
    public void dispose()
    {
        // nothing to do
    }
}
