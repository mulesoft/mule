/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mule.api.MuleException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileMoveToFunctionalTestCase extends FunctionalTestCase {

    private static final String INPUT_DIRECTORY_PROPERTY = "FileMoveToFunctionalTestCase-inputDirectory";

    private static final String MOVE_TO_DIRECTORY_PROPERTY = "FileMoveToFunctionalTestCase-moveToDirectory";

    private static final int PROBER_POLLING_INTERVAL = 100;

    private static final int PROBER_TIMEOUT = 25000;

    private static final int ITERATIONS = 10;

    private static final int NUMBER_OF_FILES = 50;

    private static final int FILE_SIZE = 1024;

    @Rule
    public TemporaryFolder inputTemporaryFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder moveToTemporaryFolder = new TemporaryFolder();

    @Override
    protected String getConfigFile() {
        return "file-functional-move-to.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        System.setProperty(INPUT_DIRECTORY_PROPERTY, inputTemporaryFolder.getRoot().getAbsolutePath());
        System.setProperty(MOVE_TO_DIRECTORY_PROPERTY, moveToTemporaryFolder.getRoot().getAbsolutePath());
    }

    @Override
    public void doTearDown() throws MuleException
    {
        System.clearProperty(INPUT_DIRECTORY_PROPERTY);
        System.clearProperty(MOVE_TO_DIRECTORY_PROPERTY);
    }

    @Test
    public void testMoveToWithStreaming() throws IOException, InterruptedException
    {
        configureConnector(true);
        copyFiles();
    }

    @Test
    public void testMoveToWithoutStreaming() throws IOException, InterruptedException
    {
        configureConnector(false);
        copyFiles();
    }

    private void configureConnector(boolean isStreaming)
    {
        FileConnector connector = (FileConnector) muleContext.getRegistry().lookupConnector("FileConnector");
        connector.setStreaming(isStreaming);
    }

    private void copyFiles() throws InterruptedException, IOException {
        for (int i=0; i < ITERATIONS; i++)
        {
            List<File> files = createFiles(inputTemporaryFolder, NUMBER_OF_FILES, FILE_SIZE);
            waitForFiles(moveToTemporaryFolder.getRoot(), i * NUMBER_OF_FILES);
        }
    }

    private List<File> createFiles(TemporaryFolder folder, int amount, int size) throws IOException
    {
        List<File> files = new ArrayList<File>(size);

        for (int i=0; i < amount; i++)
        {
            File tempInputFile = createFile(folder, String.valueOf(i), size);
            files.add(tempInputFile);
        }

        return files;
    }

    private File createFile(TemporaryFolder folder, String name, int size) throws IOException
    {
        File tempInputFile = folder.newFile("input_file_" + name);
        tempInputFile.deleteOnExit();
        byte[] content = new byte[size];
        Arrays.fill(content, (byte)0);
        FileUtils.writeByteArrayToFile(tempInputFile, content);
        return tempInputFile;
    }

    private void waitForFiles(final File folder, final int expectedAmount) throws InterruptedException
    {
        PollingProber prober = new PollingProber(PROBER_TIMEOUT, PROBER_POLLING_INTERVAL);

        prober.check(new Probe() {
            int lastAmount = 0;

            @Override
            public boolean isSatisfied() {
                lastAmount = FileUtils.listFiles(folder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).size();
                return lastAmount >= expectedAmount;
            }

            @Override
            public String describeFailure() {
                return String.valueOf(expectedAmount) + " files were expected, but only " + String.valueOf(lastAmount) + " were present.";
            }
        });
    }

}
