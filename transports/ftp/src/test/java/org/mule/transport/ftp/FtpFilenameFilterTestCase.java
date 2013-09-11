/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class FtpFilenameFilterTestCase extends AbstractFtpServerTestCase
{

    public FtpFilenameFilterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "ftp-filename-filter-config-flow.xml"}
        });
    }

    @Test
    public void filtersFile() throws Exception
    {
        File tmpDir = getFtpServerBaseDir();
        createDataFile(tmpDir, TEST_MESSAGE);

        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);

        assertNotNull("Did not processed the file", response);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }
}
