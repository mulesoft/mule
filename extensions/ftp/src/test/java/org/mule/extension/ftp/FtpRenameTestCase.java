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
import org.mule.api.MuleEvent;

import java.nio.file.Paths;

import org.junit.Test;

public class FtpRenameTestCase extends FtpConnectorTestCase
{
    private static final String RENAME_TO = "renamed";

    @Override
    protected String getConfigFile()
    {
        return "ftp-rename-config.xml";
    }

    @Test
    public void renameFile() throws Exception
    {
        createHelloWorldFile();

        doRename(HELLO_PATH);

        final String targetPath = Paths.get(HELLO_PATH).getParent().resolve(RENAME_TO).toString();

        assertThat(ftpClient.fileExists(targetPath), is((true)));
        assertThat(ftpClient.fileExists(HELLO_PATH), is((false)));
        assertThat(readPathAsString(targetPath), is(HELLO_WORLD));
    }

    @Test
    public void renameDirectory() throws Exception
    {
        createHelloWorldFile();
        final String sourcePath = Paths.get(HELLO_PATH).getParent().toString();
        doRename(sourcePath);

        assertThat(ftpClient.dirExists(sourcePath), is(false));
        assertThat(ftpClient.dirExists(RENAME_TO), is(true));

        assertThat(readPathAsString(String.format("%s/%s", RENAME_TO, HELLO_FILE_NAME)), is(HELLO_WORLD));
    }

    @Test
    public void renameUnexisting() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doRename("not-there.txt");
    }

    @Test
    public void targetAlreadyExists() throws Exception
    {
        final String sourceFile = "renameme.txt";
        ftpClient.putFile(sourceFile, ".", "rename me");
        ftpClient.putFile(RENAME_TO, ".", "I was here first");

        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doRename(sourceFile);
    }

    private void doRename(String source) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("path", source);
        event.setFlowVariable("to", RENAME_TO);

        runFlow("rename", event);
    }
}
