/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class AbstractFileRecursiveFilterTestCase extends FunctionalTestCase
{

    @Before
    public void setUpFile() throws Exception
    {
        File subfolder = FileTestUtils.createFolder(workingDirectory.getRoot(), "subfolder");

        FileTestUtils.createDataFile(subfolder, TEST_MESSAGE);
    }

    @Test
    public void filtersFiles() throws Exception
    {

        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertThat(response.getPayloadAsString(), equalTo(TEST_MESSAGE));
    }
}
