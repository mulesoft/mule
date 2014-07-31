/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.pattern.core.config;

import org.mule.tck.junit4.FunctionalTestCase;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class AbstractDeprecationTestCase extends FunctionalTestCase
{

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        LoggerConfig rootLogger = ((AbstractConfiguration) context.getConfiguration()).getRootLogger();
        Appender appender = new TestAppender("testAppender", null, null);
        context.getConfiguration().addAppender(appender);
        rootLogger.addAppender(appender, Level.WARN, null);

        context.updateLoggers();
    }
}
