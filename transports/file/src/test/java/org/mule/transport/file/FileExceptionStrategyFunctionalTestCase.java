/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;

import static edu.emory.mathcs.backport.java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FileExceptionStrategyFunctionalTestCase extends FunctionalTestCase
{
    public static final String TEST_MESSAGE = "Test file contents";
    public static final String WORKING_DIRECTORY = ".mule/temp/work-directory/";

    private Latch latch = new Latch();
    protected File inputDir;
    private SimpleFlowConstruct flow;
    private File inputFile;

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

    private void attacheLatchCountdownProcessor(String flowName)
    {
        flow = (SimpleFlowConstruct) muleContext.getRegistry().lookupFlowConstruct(flowName);
        DefaultServiceExceptionStrategy exceptionListener = (DefaultServiceExceptionStrategy) flow.getExceptionListener();
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

}
