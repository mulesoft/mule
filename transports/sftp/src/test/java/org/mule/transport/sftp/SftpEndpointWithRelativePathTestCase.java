/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.NullPayload;

public class SftpEndpointWithRelativePathTestCase extends AbstractSftpFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-endpoint-with-relative-path-config.xml";
    }


    @Test
    public void writeFileWithRelativePath() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://in", "some data", null);

        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
    }
}
