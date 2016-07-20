/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

public class FileCreateDirectoryTestCase extends FileConnectorTestCase
{

    private static final String DIRECTORY = "validDirectory";

    @Override
    protected String getConfigFile()
    {
        return "file-create-directory-config.xml";
    }

    @Test
    public void createDirectory() throws Exception
    {
        doCreateDirectory(DIRECTORY);
        assertExists(true, new File(temporaryFolder.getRoot(), DIRECTORY));
    }

    @Test
    public void createExistingDirectory() throws Exception
    {
        final String directory = "washerefirst";
        temporaryFolder.newFolder(directory);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));

        doCreateDirectory(directory);
    }

    @Test
    public void createDirectoryWithComplexPath() throws Exception
    {
        File folder = temporaryFolder.newFolder();
        doCreateDirectory(Paths.get(folder.getAbsolutePath()).resolve(DIRECTORY).toAbsolutePath().toString());

        assertExists(true, new File(folder, DIRECTORY));
    }
    private void doCreateDirectory(String directory) throws Exception
    {
        flowRunner("createDirectory").withFlowVariable("directory", directory).run();
    }

}
