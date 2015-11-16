/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.transport.file.FileTestUtils.createDataFile;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transformer.types.MimeTypes;

import org.junit.Test;

public class FileMimeTypeTestCase extends AbstractFileFunctionalTestCase
{
    private static final int TIMEOUT = 5000;

    @Override
    protected String getConfigFile()
    {
        return  "file-mime-type.xml";
    }

    @Test
    public void setsMimeType() throws Exception
    {
        createDataFile(getWorkingDirectory(), TEST_MESSAGE, null);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("vm://receive", TIMEOUT);

        assertThat(message.getDataType().getMimeType(), equalTo(MimeTypes.TEXT));
    }
}
