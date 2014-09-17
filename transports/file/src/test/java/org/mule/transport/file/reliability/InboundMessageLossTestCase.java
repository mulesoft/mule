/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.reliability;

import static org.junit.Assert.fail;
import static org.mule.transport.file.FileTestUtils.createDataFile;
import static org.mule.transport.file.FileTestUtils.createFolder;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.file.AbstractFileMoveDeleteTestCase;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Verify that no inbound messages are lost when exceptions occur. The message must
 * either make it all the way to the SEDA queue (in the case of an asynchronous
 * inbound endpoint), or be restored/rolled back at the source. In the case of the
 * File transport, this will cause the file to be restored to its original location
 * from the working directory. Note that a workDirectory must be specified on the
 * connector in order for this to succeed.
 */
public class InboundMessageLossTestCase extends AbstractFileMoveDeleteTestCase
{

    @ClassRule
    public static TemporaryFolder tempRootFolder = new TemporaryFolder();

    @Rule
    public SystemProperty tempRootFolderProperty = new SystemProperty("tempRootFolderProperty", tempRootFolder.getRoot().getAbsolutePath());

    /** Polling mechanism to replace Thread.sleep() for testing a delayed result. */
    protected Prober prober = new PollingProber(10000, 100);

    public InboundMessageLossTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // Set SystemExceptionStrategy to redeliver messages (this can only be
        // configured programatically for now)
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter(
            "*"));
    }

    @Test
    public void testNoException() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "noException");
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Delivery was successful so message should be gone
                return !file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }

    @Test
    public void testTransformerException() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "transformerException");
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Delivery failed so message should have been restored at the source
                return file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should have been restored";
            }
        });
    }

    @Test
    public void testRouterException() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "routerException");
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Delivery failed so message should have been restored at the source
                return file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should have been restored";
            }
        });
    }

    @Test
    public void testComponentException() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "componentException");
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Component exception occurs after the SEDA queue for an
                // asynchronous request, so from the client's
                // perspective, the message has been delivered successfully.
                return !file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }

    @Test
    public void testCatchExceptionStrategyConsumesMessage() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "exceptionHandled");
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Component exception occurs after the SEDA queue for an
                // asynchronous request, so from the client's
                // perspective, the message has been delivered successfully.
                return !file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }

    @Test
    public void testDefaultExceptionStrategyConsumesMessage() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "commitOnException");
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Component exception occurs after the SEDA queue for an
                // asynchronous request, so from the client's
                // perspective, the message has been delivered successfully.
                return !file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }

    @Ignore("MULE-6926: Flaky Test")
    @Test
    public void testRollbackExceptionStrategyConsumesMessage() throws Exception
    {
        final CountDownLatch exceptionStrategyLatch = new CountDownLatch(4);
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                exceptionStrategyLatch.countDown();
            }
        });
        tmpDir = createFolder(tempRootFolder.getRoot(), "rollbackOnException");
        final File file = createDataFile(tmpDir, "test1.txt");
        if (!exceptionStrategyLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should be redelivered");
        }
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return !file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }

}
