/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.hamcrest.CoreMatchers.instanceOf;
import org.mule.api.MuleEvent;

import java.io.File;

import org.junit.Test;

public class FileCreateDirectoryTestCase extends FileConnectorTestCase
{

    private static final String DIRECTORY = "a/b/c";

    @Override
    protected String getConfigFile()
    {
        return "file-create-directory-config.xml";
    }

    @Test
    public void createDirectory() throws Exception
    {
        doCreateDirectory(null, DIRECTORY);
        assertExists(true, new File(temporaryFolder.getRoot(), DIRECTORY));
    }

    @Test
    public void createExistingDirectory() throws Exception
    {
        final String directory = "washerefirst";
        temporaryFolder.newFolder(directory);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        doCreateDirectory(null, directory);
    }

    @Test
    public void createDirectoryWithCustomBasePath() throws Exception
    {
        File base = temporaryFolder.getRoot();
        doCreateDirectory(base.getAbsolutePath(), DIRECTORY);

        assertExists(true, new File(base, DIRECTORY));
    }

    private void doCreateDirectory(String basePath, String directory) throws Exception
    {
        String flowName;
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("directory", directory);
        if (basePath != null)
        {
            flowName = "renameWithBasePath";
            event.setFlowVariable("basePath", basePath);
        }
        else
        {
            flowName = "rename";
        }

        runFlow(flowName, event);
    }

}
