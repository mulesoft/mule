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

import org.junit.Test;

public class FtpCreateDirectoryTestCase extends FtpConnectorTestCase
{
    private static final String DIRECTORY = "a/b/c";

    @Override
    protected String getConfigFile()
    {
        return "ftp-create-directory-config.xml";
    }

    @Test
    public void createDirectory() throws Exception
    {
        doCreateDirectory(null, DIRECTORY);
        assertThat(ftpClient.dirExists(DIRECTORY), is(true));
    }

    @Test
    public void createExistingDirectory() throws Exception
    {
        final String directory = "washerefirst";
        ftpClient.makeDir(directory);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        doCreateDirectory(null, directory);
    }

    @Test
    public void createDirectoryWithCustomBasePath() throws Exception
    {
        final String base = ftpClient.getWorkingDirectory();
        doCreateDirectory(base, DIRECTORY);

        assertThat(ftpClient.dirExists(DIRECTORY), is(true));
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
