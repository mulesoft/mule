/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.FileUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class FileExceptionStrategyFunctionalTestCase extends FunctionalTestCase
{
    public static final String TEST_MESSAGE = "Test file contents";

    public static final String FILE_WORKING_DIRECTORY_FOLDER = "temp/work-directory/";

    private Latch latch = new Latch();
    protected File inputDir;
    private Flow flow;
    private File inputFile;
    private PollingProber pollingProber = new PollingProber(5000, 200);

    @Override
    protected String getConfigFile()
    {
        return "file-exception-strategy-config.xml";
    }

    @Test
    public void testMoveFile() throws Exception
    {
        attacheLatchCountdownProcessor("moveFile");
        inputDir = getFileInsideWorkingDirectory("temp/input-move-file");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = getFileInsideWorkingDirectory("temp/output-directory/" + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(true));
    }

    @Test
    public void testMoveFileWithWorDir() throws Exception
    {
        attacheLatchCountdownProcessor("moveFileWithWorkDir");
        inputDir = getFileInsideWorkingDirectory("temp/input-move-file-wd");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = getFileInsideWorkingDirectory("temp/output-directory/" + inputFile.getName());
        File workDirFile = getFileInsideWorkingDirectory(FILE_WORKING_DIRECTORY_FOLDER + File.separator + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(true));
        assertThat(workDirFile.exists(), is(false));
    }


    @Test
    public void testCopyFile() throws Exception
    {
        attacheLatchCountdownProcessor("copyFile");
        inputDir = getFileInsideWorkingDirectory("temp/input-copy-file");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = getFileInsideWorkingDirectory("temp/output-directory/" + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(false));
    }


    @Test
    public void testCopyFileWithWorkDir() throws Exception
    {
        attacheLatchCountdownProcessor("copyFileWithWorkDir");
        inputDir = getFileInsideWorkingDirectory("temp/input-copy-file-with-work-directory");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = getFileInsideWorkingDirectory("temp/output-directory/" + inputFile.getName());
        File workDirFile = getFileInsideWorkingDirectory(FILE_WORKING_DIRECTORY_FOLDER + File.separator + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(false));
        assertThat(workDirFile.exists(), is(false));
    }

    @Test
    public void testConsumeFileWithExAndCatch() throws Exception
    {
        inputDir = getFileInsideWorkingDirectory("temp/input-streaming-catch");
        inputFile = createDataFile(inputDir, "test1.txt");
        pollingProber.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return !inputFile.exists();
            }

            @Override
            public String describeFailure()
            {
                return "input file should be deleted";
            }
        });
    }

    @Test
    public void testConsumeFileWithExAndRollback() throws Exception
    {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        FunctionalTestComponent ftc = getFunctionalTestComponent("consumeFileWithStreamingAndRollback");
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                countDownLatch.countDown();
                throw new RuntimeException();
            }
        });
        inputDir = getFileInsideWorkingDirectory("temp/input-streaming-rollback");
        inputFile = createDataFile(inputDir, "test1.txt");

        if (!countDownLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("file should not be consumed");
        }
    }

    @Test
    public void testConsumeFileWithExAndRollbackWithRedelivery() throws Exception
    {
        final CountDownLatch countDownLatch = new CountDownLatch(3);
        FunctionalTestComponent ftc = getFunctionalTestComponent("consumeFileWithStreamingAndRollbackWithRedelivery");
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                countDownLatch.countDown();
                throw new RuntimeException();
            }
        });
        inputDir = getFileInsideWorkingDirectory("temp/input-streaming-rollback-with-redelivery");
        inputFile = createDataFile(inputDir, "test1.txt");
        if (!countDownLatch.await(100000, TimeUnit.MILLISECONDS))
        {
            fail("file should not be consumed at this point");
        }
        pollingProber.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return !inputFile.exists();
            }

            @Override
            public String describeFailure()
            {
                return "input file should be deleted";
            }
        });
    }
    
    @Test
    public void testConsumeFileWithAsynchronousProcessingStrategy() throws Exception
    {
        inputDir = getFileInsideWorkingDirectory("temp/input-streaming-and-async-processing-strategy");
        inputFile = createDataFile(inputDir, "test1.txt");
        BeforeCloseStream.releaseLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        assertThat(inputFile.exists(),is(true));
        BeforeCloseStream.awaitLatch.release();
        AfterCloseStream.releaseLatch.await(RECEIVE_TIMEOUT,TimeUnit.MILLISECONDS);
        pollingProber.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return !inputFile.exists();
            }

            @Override
            public String describeFailure()
            {
                return "input file should be deleted";
            }
        });
    }

    private void attacheLatchCountdownProcessor(String flowName)
    {
        flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        DefaultMessagingExceptionStrategy exceptionListener = (DefaultMessagingExceptionStrategy) flow.getExceptionListener();
        exceptionListener.getMessageProcessors().add(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                latch.countDown();
                return event;
            }
        });
    }

    @Override
    @Before
    public void doSetUp()
    {
        getFileInsideWorkingDirectory(FILE_WORKING_DIRECTORY_FOLDER).mkdirs();
    }

    protected File createDataFile(File folder, final String testMessage) throws Exception
    {
        return createDataFile(folder, testMessage, null);
    }

    protected File createDataFile(File folder, final String testMessage, String encoding) throws Exception
    {
        folder.mkdirs();
        File target = File.createTempFile("data", ".txt", folder);
        target.deleteOnExit();
        FileUtils.writeStringToFile(target, testMessage, encoding);
        return target;
    }

    public static class BeforeCloseStream implements MessageProcessor
    {
        public static Latch releaseLatch = new Latch();
        public static Latch awaitLatch = new Latch();
        public File file;
        
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            releaseLatch.release();
            try
            {
                awaitLatch.await(RECEIVE_TIMEOUT,TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            return event;
        }
    }

    public static class AfterCloseStream implements MessageProcessor
    {
        public static Latch releaseLatch = new Latch();
        public File file;
        
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            releaseLatch.release();
            return event;
        }
    }

}
