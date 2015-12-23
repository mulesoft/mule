/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.extension.file.api.FileWriteMode.APPEND;
import static org.mule.module.extension.file.api.FileWriteMode.CREATE_NEW;
import static org.mule.module.extension.file.api.FileWriteMode.OVERWRITE;
import org.mule.module.extension.file.api.FileWriteMode;
import org.mule.util.IOUtils;

import java.nio.file.Paths;

import org.junit.Test;

public class FtpWriteTestCase extends FtpConnectorTestCase
{

    private static final String TEMP_DIRECTORY = "files";

    @Override
    protected String getConfigFile()
    {
        return "ftp-write-config.xml";
    }

    @Test
    public void appendOnNotExistingFile() throws Exception
    {
        doWriteOnNotExistingFile(APPEND);
    }

    @Test
    public void overwriteOnNotExistingFile() throws Exception
    {
        doWriteOnNotExistingFile(OVERWRITE);
    }

    @Test
    public void createNewOnNotExistingFile() throws Exception
    {

        doWriteOnNotExistingFile(FileWriteMode.CREATE_NEW);
    }

    @Test
    public void appendOnExistingFile() throws Exception
    {
        String content = doWriteOnExistingFile(APPEND);
        assertThat(content, is(HELLO_WORLD + HELLO_WORLD));
    }

    @Test
    public void overwriteOnExistingFile() throws Exception
    {
        String content = doWriteOnExistingFile(OVERWRITE);
        assertThat(content, is(HELLO_WORLD));
    }

    @Test
    public void createNewOnExistingFile() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doWriteOnExistingFile(CREATE_NEW);
    }

    @Test
    public void appendOnNotExistingParentWithoutCreateFolder() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doWriteOnNotExistingParentWithoutCreateFolder(APPEND);
    }

    @Test
    public void overwriteOnNotExistingParentWithoutCreateFolder() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doWriteOnNotExistingParentWithoutCreateFolder(OVERWRITE);
    }

    @Test
    public void createNewOnNotExistingParentWithoutCreateFolder() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doWriteOnNotExistingParentWithoutCreateFolder(CREATE_NEW);
    }

    @Test
    public void appendNotExistingFileWithCreatedParent() throws Exception
    {
        doWriteNotExistingFileWithCreatedParent(APPEND);
    }

    @Test
    public void overwriteNotExistingFileWithCreatedParent() throws Exception
    {
        doWriteNotExistingFileWithCreatedParent(OVERWRITE);
    }

    @Test
    public void createNewNotExistingFileWithCreatedParent() throws Exception
    {
        doWriteNotExistingFileWithCreatedParent(CREATE_NEW);
    }

    private void doWriteNotExistingFileWithCreatedParent(FileWriteMode mode) throws Exception
    {
        ftpClient.makeDir(TEMP_DIRECTORY);
        String path = Paths.get(ftpClient.getWorkingDirectory(), TEMP_DIRECTORY, "a/b/test.txt").toString();

        doWrite(path, HELLO_WORLD, mode, true);

        String content = IOUtils.toString(readPath(path).getContent());
        assertThat(content, is(HELLO_WORLD));
    }


    private void doWriteOnNotExistingFile(FileWriteMode mode) throws Exception
    {
        ftpClient.makeDir(TEMP_DIRECTORY);
        String path = Paths.get(ftpClient.getWorkingDirectory(), TEMP_DIRECTORY, "test.txt").toString();
        doWrite(path, HELLO_WORLD, mode, false);

        String content = IOUtils.toString(readPath(path).getContent());
        assertThat(content, is(HELLO_WORLD));
    }

    private void doWriteOnNotExistingParentWithoutCreateFolder(FileWriteMode mode) throws Exception
    {
        ftpClient.makeDir(TEMP_DIRECTORY);
        String path = Paths.get(ftpClient.getWorkingDirectory(), TEMP_DIRECTORY, "a/b/test.txt").toString();
        doWrite(path, HELLO_WORLD, mode, false);
    }

    private String doWriteOnExistingFile(FileWriteMode mode) throws Exception
    {
        final String filePath = "file";
        ftpClient.putFile(filePath, ".", HELLO_WORLD);

        doWrite(filePath, HELLO_WORLD, mode, false);
        return IOUtils.toString(readPath(filePath).getContent());
    }
}
