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
import org.mule.api.MuleEvent;

import java.io.File;

import org.junit.Test;

public class FileRenameTestCase extends FileConnectorTestCase
{

    private static final String RENAME_TO = "renamed";

    @Override
    protected String getConfigFile()
    {
        return "file-rename-config.xml";
    }

    @Test
    public void renameFile() throws Exception
    {
        File origin = createHelloWorldFile();

        doRename(origin.getAbsolutePath());
        File expected = new File(origin.getParent(), RENAME_TO);

        assertExists(false, origin);
        assertExists(true, expected);
        assertThat(readPathAsString(expected.getAbsolutePath()), is(HELLO_WORLD));
    }

    @Test
    public void renameDirectory() throws Exception
    {
        File origin = createHelloWorldFile().getParentFile();
        doRename(origin.getAbsolutePath());

        File expected = new File(origin.getParent(), RENAME_TO);

        assertExists(false, origin);
        assertExists(true, expected);

        assertThat(readPathAsString(String.format("%s/%s", expected.getAbsolutePath(), HELLO_FILE_NAME)), is(HELLO_WORLD));
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
        File origin = temporaryFolder.newFile("source");
        temporaryFolder.newFile(RENAME_TO);

        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        doRename(origin.getAbsolutePath());

    }

    private void doRename(String source) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("path", source);
        event.setFlowVariable("to", RENAME_TO);

        runFlow("rename", event);
    }
}
