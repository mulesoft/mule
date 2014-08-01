/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import static org.junit.Assert.assertNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class FileNoRecursiveConnectorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "file-no-recursive-connector-config.xml";
    }

    @Before
    public void setUpFile() throws Exception
    {
        File root = FileTestUtils.createFolder(workingDirectory.getRoot(), "root");
        File subfolder = FileTestUtils.createFolder(root, "subfolder");
        FileTestUtils.createDataFile(subfolder, TEST_MESSAGE);
    }

    @Test
    public void findsInRootDirectoryOnly() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.request("vm://testOut", RECEIVE_TIMEOUT);

        assertNull("Found a file from a sub directory", result);
    }
}
