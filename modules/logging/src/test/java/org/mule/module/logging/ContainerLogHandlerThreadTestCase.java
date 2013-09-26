/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.logging;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ContainerLogHandlerThreadTestCase extends AbstractLogHandlerThreadTestCase
{

    public ContainerLogHandlerThreadTestCase(LoggerFactoryFactory loggerFactory, String logHandlerThreadName)
    {
        super(loggerFactory, logHandlerThreadName);
    }

    @Test
    public void doesNotStarsLogHandlerThreadOnContainerMode() throws Exception
    {
        loggerFactory.create();

        assertFalse("Created an unexpected LoggerReferenceHandler instance", createdLoggerReferenceHandler);
    }
}
