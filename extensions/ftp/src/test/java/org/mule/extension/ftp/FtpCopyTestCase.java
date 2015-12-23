/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;

import java.nio.file.Paths;

import org.junit.Test;

public class FtpCopyTestCase extends FtpConnectorTestCase
{

    private static final String SOURCE_FILE_NAME = "test.txt";
    private static final String SOURCE_DIRECTORY_NAME = "source";
    private static final String TARGET_DIRECTORY = "target";
    private static final String EXISTING_CONTENT = "I was here first!";

    protected String sourcePath;

    @Override
    protected String getConfigFile()
    {
        return "ftp-copy-config.xml";
    }

    private String getPath(String... path) throws Exception
    {
        return Paths.get(ftpClient.getWorkingDirectory(), path).toString();
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        ftpClient.putFile(SOURCE_FILE_NAME, ".", HELLO_WORLD);
        sourcePath = getPath(SOURCE_FILE_NAME);
    }

    @Test
    public void toExistingFolder() throws Exception
    {
        ftpClient.makeDir(TARGET_DIRECTORY);
        final String path = getPath(TARGET_DIRECTORY);
        doExecute(path, false, false);

        assertCopy(format("%s/%s", path, SOURCE_FILE_NAME));
    }

    @Test
    public void toNonExistingFolder() throws Exception
    {
        ftpClient.makeDir(TARGET_DIRECTORY);
        String target = format("%s/%s", TARGET_DIRECTORY, "a/b/c");
        doExecute(target, false, true);

        assertCopy(format("%s/%s", target, SOURCE_FILE_NAME));
    }

    @Test
    public void toNonExistingFolderWithoutCreateParent() throws Exception
    {
        ftpClient.makeDir(TARGET_DIRECTORY);
        String target = format("%s/%s", TARGET_DIRECTORY, "a/b/c");
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doExecute(target, false, false);
    }

    @Test
    public void overwrite() throws Exception
    {
        final String existingFileName = "existing";
        ftpClient.putFile(existingFileName, ".", EXISTING_CONTENT);

        final String target = getPath(existingFileName);

        doExecute(target, true, false);
        assertCopy(target);
    }

    @Test
    public void withoutOverwrite() throws Exception
    {
        final String existingFileName = "existing";
        ftpClient.putFile(existingFileName, ".", EXISTING_CONTENT);
        final String target = getPath(existingFileName);

        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doExecute(target, false, false);
    }

    @Test
    public void directoryToExistingDirectory() throws Exception
    {
        sourcePath = buildSourceDirectory();
        final String target = "target";
        ftpClient.makeDir(target);
        doExecute(target, false, false);
        assertCopy(format("%s/source/%s", target, SOURCE_FILE_NAME));
    }

    @Test
    public void directoryToNotExistingDirectory() throws Exception
    {
        sourcePath = buildSourceDirectory();

        String target = "a/b/c";
        doExecute(target, false, true);

        assertCopy(format("%s/source/%s", target, SOURCE_FILE_NAME));
    }

    @Test
    public void directoryAndOverwrite() throws Exception
    {
        sourcePath = buildSourceDirectory();

        final String target = "target";
        ftpClient.makeDir(target);
        ftpClient.changeWorkingDirectory(target);
        ftpClient.makeDir(SOURCE_DIRECTORY_NAME);
        ftpClient.putFile(SOURCE_FILE_NAME, SOURCE_DIRECTORY_NAME, EXISTING_CONTENT);

        ftpClient.changeWorkingDirectory("../");

        doExecute(target, true, false);
        assertCopy(format("%s/%s/%s", target, SOURCE_DIRECTORY_NAME, SOURCE_FILE_NAME));
    }

    @Test
    public void directoryWithoutOverwrite() throws Exception
    {
        sourcePath = buildSourceDirectory();

        final String target = "target";
        ftpClient.makeDir(target);
        ftpClient.changeWorkingDirectory(target);
        ftpClient.makeDir(SOURCE_DIRECTORY_NAME);
        ftpClient.putFile(SOURCE_FILE_NAME, SOURCE_DIRECTORY_NAME, EXISTING_CONTENT);
        ftpClient.changeWorkingDirectory("../");

        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doExecute(format("%s/%s", target, SOURCE_DIRECTORY_NAME), false, false);
    }

    @Test
    public void copyWithDifferentName() throws Exception
    {
        ftpClient.makeDir(TARGET_DIRECTORY);
        String target = getPath(TARGET_DIRECTORY) + "/test.json";
        doExecute(target, false, false);

        assertCopy(target);
    }

    private String buildSourceDirectory() throws Exception
    {
        ftpClient.makeDir(SOURCE_DIRECTORY_NAME);
        ftpClient.putFile(SOURCE_FILE_NAME, SOURCE_DIRECTORY_NAME, HELLO_WORLD);

        return getPath(SOURCE_DIRECTORY_NAME);
    }

    private void doExecute(String target, boolean overwrite, boolean createParentFolder) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable(SOURCE_DIRECTORY_NAME, sourcePath);
        event.setFlowVariable("target", target);
        event.setFlowVariable("overwrite", overwrite);
        event.setFlowVariable("createParent", createParentFolder);

        runFlow(getFlowName(), event);
    }

    protected void assertCopy(String target) throws Exception
    {
        assertThat(readPathAsString(target), equalTo(HELLO_WORLD));
    }

    @Override
    protected String readPathAsString(String path) throws Exception
    {
        return super.readPathAsString(path);
    }

    protected String getFlowName()
    {
        return "copy";
    }
}
