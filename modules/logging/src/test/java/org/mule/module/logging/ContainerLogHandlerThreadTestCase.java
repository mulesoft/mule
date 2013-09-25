/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.logging;

import static org.junit.Assert.fail;

import org.mule.tck.probe.thread.ThreadExists;

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

        try
        {
            prober.check(new ThreadExists(logHandlerThreadName));

            fail("Log handler thread is not supposed be started when Mule is running on container mode");
        }
        catch (AssertionError e)
        {
            // Expected
        }
    }
}
