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
import org.mule.extension.FtpTestHarness;
import org.mule.functional.junit4.FlowRunner;

import org.junit.Test;

public class FtpCreateDirectoryTestCase extends FtpConnectorTestCase
{

    private static final String DIRECTORY = "a/b/c";

    public FtpCreateDirectoryTestCase(String name, FtpTestHarness testHarness)
    {
        super(name, testHarness);
    }

    @Override
    protected String getConfigFile()
    {
        return "ftp-create-directory-config.xml";
    }

    @Test
    public void createDirectory() throws Exception
    {
        doCreateDirectory(null, DIRECTORY);
        assertThat(testHarness.dirExists(DIRECTORY), is(true));
    }

    @Test
    public void createExistingDirectory() throws Exception
    {
        final String directory = "washerefirst";
        testHarness.makeDir(directory);
        testHarness.expectedException().expectCause(instanceOf(IllegalArgumentException.class));

        doCreateDirectory(null, directory);
    }

    @Test
    public void createDirectoryWithCustomBasePath() throws Exception
    {
        final String base = testHarness.getWorkingDirectory();
        doCreateDirectory(base, DIRECTORY);

        assertThat(testHarness.dirExists(DIRECTORY), is(true));
    }

    private void doCreateDirectory(String basePath, String directory) throws Exception
    {
        FlowRunner runner;
        if (basePath != null)
        {
            runner = flowRunner("renameWithBasePath").withFlowVariable("basePath", basePath);
        }
        else
        {
            runner = flowRunner("rename");
        }

        runner.withFlowVariable("directory", directory).run();
    }
}
