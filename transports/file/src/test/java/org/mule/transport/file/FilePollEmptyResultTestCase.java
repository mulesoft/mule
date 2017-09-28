/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.util.FileUtils.openDirectory;

import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import org.junit.Before;
import org.junit.Test;


public class FilePollEmptyResultTestCase extends FunctionalTestCase
{

    private static long TEST_TIMEOUT = 5000;

    private Latch latch = new Latch();

    private static Throwable concurrentException;

    @Override
    protected String getConfigFile()
    {
        return "file-poll-empty-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        openDirectory(getFileInsideWorkingDirectory("test").getAbsolutePath());
        openDirectory(getFileInsideWorkingDirectory("test2").getAbsolutePath());
        muleContext.setExceptionListener(new TestExceptionHandler());
    }

    @Test
    public void testNoConcurrentExceptionWhenEmptyPollResult() throws InterruptedException
    {
        assertThat(latch.await(TEST_TIMEOUT, MILLISECONDS), is(false));
        assertThat(concurrentException, is(nullValue()));
    }

    private class TestExceptionHandler implements SystemExceptionHandler
    {

        @Override
        public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
        {

        }

        @Override
        public void handleException(Exception exception)
        {
            concurrentException = exception;
            latch.countDown();
        }
    }


}
