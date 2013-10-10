/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import org.mule.tck.probe.Prober;
import org.mule.util.FileUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FileExceptionStrategyFunctionalTestCase extends FunctionalTestCase
{
    public static final String TEST_MESSAGE = "Test file contents";
    public static final String WORKING_DIRECTORY = ".mule/temp/work-directory/";

    private Latch latch = new Latch();
    protected File inputDir;
    private Flow flow;
    private File inputFile;
    private PollingProber pollingProber = new PollingProber(5000, 200);

    @Override
    protected String getConfigResources()
    {
        return "file-exception-strategy-config.xml";
    }

    @Test
    public void testMoveFile() throws Exception
    {
        attacheLatchCountdownProcessor("moveFile");
        inputDir = new File(".mule/temp/input-move-file");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = new File(".mule/temp/output-directory/" + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(true));
    }

    @Test
    public void testMoveFileWithWorDir() throws Exception
    {
        attacheLatchCountdownProcessor("moveFileWithWorkDir");
        inputDir = new File(".mule/temp/input-move-file-wd");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = new File(".mule/temp/output-directory/" + inputFile.getName());
        File workDirFile = new File(WORKING_DIRECTORY + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(true));
        assertThat(workDirFile.exists(), is(false));
    }


    @Test
    public void testCopyFile() throws Exception
    {
        attacheLatchCountdownProcessor("copyFile");
        inputDir = new File(".mule/temp/input-copy-file");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = new File(".mule/temp/output-directory/" + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(false));
    }


    @Test
    public void testCopyFileWithWorkDir() throws Exception
    {
        attacheLatchCountdownProcessor("copyFileWithWorkDir");
        inputDir = new File(".mule/temp/input-copy-file-with-work-directory");
        inputFile = createDataFile(inputDir, "test1.txt");
        latch.await(2000l, MILLISECONDS);
        flow.stop();
        File outputFile = new File(".mule/temp/output-directory/" + inputFile.getName());
        File workDirFile = new File(WORKING_DIRECTORY + inputFile.getName());
        assertThat(inputFile.exists(), is(false));
        assertThat(outputFile.exists(), is(false));
        assertThat(workDirFile.exists(), is(false));
    }

    @Test
    public void testConsumeFileWithExAndCatch() throws Exception
    {
        inputDir = new File(".mule/temp/input-streaming-catch");
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
        inputDir = new File(".mule/temp/input-streaming-rollback");
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
        inputDir = new File(".mule/temp/input-streaming-rollback-with-redelivery");
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
        inputDir = new File(".mule/temp/input-streaming-and-async-processing-strategy");
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

    @Before
    public void doSetUp()
    {
        FileUtils.deleteTree(new File("./mule/temp"));
    }

    @After
    public void tearDown()
    {
        FileUtils.deleteTree(new File("./mule/temp"));
    }

    protected File createDataFile(File folder, final String testMessage) throws Exception
    {
        return createDataFile(folder, testMessage, null);
    }

    protected File createDataFile(File folder, final String testMessage, String encoding) throws Exception
    {
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
