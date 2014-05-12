/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.Preconditions;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class UntilSuccessfulWithPersistentObjectStoreTestCase extends FunctionalTestCase
{

    private static final int TIMEOUT = 20000;
    private static boolean shouldFail = false;
    private static Latch latch = null;


    @Override
    protected String getConfigFile()
    {
        return "until-successful-with-persistent-os-config.xml";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        latch = null;
    }

    /**
     * This test verifies that if mule dies before a stored event if successfully delivered,
     * Mule will actually reattempt upon restart
     */
    @Test
    @Ignore("MULE-6926: flaky test")
    public void recoverMuleEvent() throws Exception
    {
        shouldFail = true;
        latch = new Latch();
        MuleEvent event = getTestEvent("");
        muleContext.getClient().dispatch("vm://recover", event.getMessage());

        assertTrue(latch.await(TIMEOUT, SECONDS));
        muleContext.dispose();
        muleContext = null;

        TemporaryFolder originalWorkingDirectory = workingDirectory;

        // avoid recreation of working directory to preserve messages
        workingDirectory = new TemporaryFolderWrapper(workingDirectory);

        latch = new Latch();
        shouldFail = false;

        try
        {
            setUpMuleContext();

            assertTrue(latch.await(TIMEOUT, MILLISECONDS));
            assertNotNull(muleContext.getClient().request("vm://success", TIMEOUT));
        }
        finally
        {
            workingDirectory = originalWorkingDirectory;
        }
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        MuleContext ctx = super.createMuleContext();
        ctx.setExecutionClassLoader(getClass().getClassLoader());

        return ctx;
    }

    public static class FailingTestMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                Preconditions.checkState(!shouldFail, "Failing on purpose");
                return event;
            }
            finally
            {
                latch.release();
            }
        }
    }

    private class TemporaryFolderWrapper extends TemporaryFolder
    {

        private final TemporaryFolder wrapped;

        private TemporaryFolderWrapper(TemporaryFolder wrapped)
        {
            this.wrapped = wrapped;
        }

        @Override
        public void before() throws Throwable
        {
            // do nothing
        }

        @Override
        public void after()
        {
            // do nothing
        }

        @Override
        public void create() throws IOException
        {
            // do nothing
        }

        @Override
        public File newFile(String fileName) throws IOException
        {
            return wrapped.newFile(fileName);
        }

        @Override
        public File newFolder(String folderName)
        {
            return wrapped.newFolder(folderName);
        }

        @Override
        public File getRoot()
        {
            return wrapped.getRoot();
        }

        @Override
        public void delete()
        {
            wrapped.delete();
        }

        @Override
        public Statement apply(Statement base, Description description)
        {
            return wrapped.apply(base, description);
        }
    }
}
