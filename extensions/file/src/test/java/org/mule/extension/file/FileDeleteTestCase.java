/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Test;

public class FileDeleteTestCase extends FileConnectorTestCase
{

    private static final String GRAND_CHILD = "grandChild";
    private static final String DELETE = "delete";

    @Override
    protected String getConfigFile()
    {
        return "file-delete-config.xml";
    }

    @Test
    public void deleteFile() throws Exception
    {
        File file = temporaryFolder.newFile();
        assertExists(true, file);

        flowRunner(DELETE).withFlowVariable(DELETE, file.getAbsolutePath()).run();

        assertExists(false, file);
    }

    @Test
    public void deleteFolder() throws Exception
    {
        File directory = temporaryFolder.newFolder();
        File child = new File(directory, "file");
        FileUtils.write(child, "child");

        File subFolder = new File(directory, "subfolder");
        subFolder.mkdir();
        File grandChild = new File(subFolder, GRAND_CHILD);
        FileUtils.write(grandChild, GRAND_CHILD);

        assertExists(true, child, subFolder, grandChild);

        flowRunner(DELETE).withFlowVariable(DELETE, directory.getAbsolutePath()).run();
        assertExists(false, directory, child, subFolder, grandChild);
    }

}
