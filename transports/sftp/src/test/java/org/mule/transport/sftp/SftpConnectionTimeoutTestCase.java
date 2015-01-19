/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;
import org.mule.transport.sftp.util.SftpServer;
import org.mule.util.concurrent.Latch;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.junit.Rule;
import org.junit.Test;

public class SftpConnectionTimeoutTestCase extends FunctionalTestCase
{

    @Rule
    public final DynamicPort sftpPort = new DynamicPort("sftpPort");

    @Override
    protected String getConfigResources()
    {
        return "sftp-connection-timeout-config.xml";
    }

    @Test
    public void timeoutsSimpleConnection() throws Exception
    {
        doTest("vm://simpleLogin");
    }

    @Test
    public void timeoutsPassPhraseConnection() throws Exception
    {
        doTest("vm://passphraseLogin");
    }

    private void doTest(String url) throws MuleException
    {
        SftpServer server = new SftpServer(sftpPort.getNumber());
        Latch latch = new Latch();
        try
        {
            server.setPasswordAuthenticator(new BlockingPasswordAuthenticator(latch));
            server.start();
            MuleClient client = muleContext.getClient();
            MuleMessage result = client.send(url, TEST_MESSAGE, null);
            assertThat(result.getExceptionPayload(), not(nullValue()));
            assertThat((NullPayload) result.getPayload(), is(NullPayload.getInstance()));
        }
        finally
        {
            latch.release();
            server.stop();
        }
    }

    public static class BlockingPasswordAuthenticator implements PasswordAuthenticator
    {

        private Latch latch;

        public BlockingPasswordAuthenticator(Latch latch)
        {
            this.latch = latch;
        }

        @Override
        public boolean authenticate(String username, String password, ServerSession session)
        {
            try
            {
                latch.await();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            return false;
        }
    }
}
