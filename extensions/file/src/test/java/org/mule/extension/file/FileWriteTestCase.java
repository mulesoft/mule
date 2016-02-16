/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.extension.file.api.FileWriteMode.APPEND;
import static org.mule.module.extension.file.api.FileWriteMode.CREATE_NEW;
import static org.mule.module.extension.file.api.FileWriteMode.OVERWRITE;
import org.mule.module.extension.file.api.FileWriteMode;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Test;

public class FileWriteTestCase extends FileConnectorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "file-write-config.xml";
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
        File folder = temporaryFolder.newFolder();
        final String path = folder.getAbsolutePath() + "/a/b/test.txt";

        doWrite(path, HELLO_WORLD, mode, true);

        String content = readPathAsString(path);
        assertThat(content, is(HELLO_WORLD));
    }


    private void doWriteOnNotExistingFile(FileWriteMode mode) throws Exception
    {
        String path = temporaryFolder.newFolder().getPath() + "/test.txt";
        doWrite(path, HELLO_WORLD, mode, false);

        String content = readPathAsString(path);
        assertThat(content, is(HELLO_WORLD));
    }

    private void doWriteOnNotExistingParentWithoutCreateFolder(FileWriteMode mode) throws Exception
    {
        File folder = temporaryFolder.newFolder();
        final String path = folder.getAbsolutePath() + "/a/b/test.txt";

        doWrite(path, HELLO_WORLD, mode, false);
    }

    private String doWriteOnExistingFile(FileWriteMode mode) throws Exception
    {
        File file = temporaryFolder.newFile();
        FileUtils.writeStringToFile(file, HELLO_WORLD);

        doWrite(file.getAbsolutePath(), HELLO_WORLD, mode, false);
        return readPathAsString(file.getAbsolutePath());
    }
}
