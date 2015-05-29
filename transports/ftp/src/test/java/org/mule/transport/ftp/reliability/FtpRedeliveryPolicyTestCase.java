/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp.reliability;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.listener.ExceptionListener;
import org.mule.transport.ftp.AbstractFtpServerTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class FtpRedeliveryPolicyTestCase extends AbstractFtpServerTestCase
{

    private static final String FILE_TXT = "file.txt";
    private static final int MAX_REDELIVERY_ATTEMPTS = 2;

    @Rule
    public SystemProperty maxRedeliveryAttemptsSystemProperty = new SystemProperty("maxRedeliveryAttempts", Integer.toString(MAX_REDELIVERY_ATTEMPTS));

    public FtpRedeliveryPolicyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "reliability/ftp-redelivery-policy-config.xml"}
        });
    }
    @Test
    public void testRedeliveryPolicyDLQConsumesMessage() throws Exception
    {
        ExceptionListener exceptionListener = new ExceptionListener(muleContext).setNumberOfExecutionsRequired(MAX_REDELIVERY_ATTEMPTS + 1);
        createFileOnFtpServer(FILE_TXT);
        exceptionListener.waitUntilAllNotificationsAreReceived();
        MuleMessage message = muleContext.getClient().request("vm://error-queue", RECEIVE_TIMEOUT);
        assertThat(message, is(notNullValue()));
        assertThat(message.getPayloadAsString(), is(TEST_MESSAGE));
        assertThat(fileExists(FILE_TXT), is(false));
    }
}
