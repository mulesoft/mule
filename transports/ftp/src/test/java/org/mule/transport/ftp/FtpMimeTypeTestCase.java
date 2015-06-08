/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transformer.types.MimeTypes;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class FtpMimeTypeTestCase extends AbstractFtpServerTestCase
{

    private static final int TIMEOUT = 5000;

    public FtpMimeTypeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "ftp-mime-type.xml"}
        });
    }

    @Test
    public void setsMimeType() throws Exception
    {
        createFileOnFtpServer("test.txt");

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("vm://receive", TIMEOUT);

        assertThat(message.getDataType().getMimeType(), equalTo(MimeTypes.TEXT));
    }
}
