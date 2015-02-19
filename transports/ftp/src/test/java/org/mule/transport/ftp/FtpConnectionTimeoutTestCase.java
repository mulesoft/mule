/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.util.ftp.Server;
import org.mule.transport.NullPayload;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.impl.AbstractUserManager;
import org.junit.Rule;
import org.junit.Test;

public class FtpConnectionTimeoutTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort ftpPort = new DynamicPort("ftpPort");

    @Override
    protected String getConfigFile()
    {
        return "ftp-connection-timeout-config-flow.xml";
    }

    @Test
    public void timeoutsConnection() throws Exception
    {
        CountDownLatch latch = new CountDownLatch(1);

        Server server = new BlockingFtpServer(ftpPort.getNumber(), latch);

        try
        {
            MuleClient client = muleContext.getClient();
            MuleMessage result = client.send("vm://in", "somethingFTPFail", null);
            assertThat(result, is(not(nullValue())));
            assertThat(result.getExceptionPayload(), is(not(nullValue())));
            assertThat(result.getPayload(), instanceOf(NullPayload.class));
        }
        finally
        {
            latch.countDown();

            server.stop();
        }
    }

    private static class BlockingFtpServer extends Server
    {

        private final CountDownLatch latch;

        public BlockingFtpServer(int port, CountDownLatch latch) throws Exception
        {
            super(port);
            this.latch = latch;
        }

        @Override
        protected UserManager createUserManager() throws IOException
        {
            return new BlockingFtpUserManager(latch);
        }
    }

    private static class BlockingFtpUserManager extends AbstractUserManager
    {

        private final CountDownLatch latch;

        public BlockingFtpUserManager(CountDownLatch latch)
        {
            super(null, null);
            this.latch = latch;
        }

        @Override
        public User authenticate(Authentication inAuth)
                throws AuthenticationFailedException
        {
            try
            {
                latch.await();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        public void delete(String arg0) throws FtpException
        {
        }

        @Override
        public boolean doesExist(String arg0) throws FtpException
        {
            return false;
        }


        @Override
        public String[] getAllUserNames() throws FtpException
        {
            return new String[0];
        }

        @Override
        public User getUserByName(String userName) throws FtpException
        {
            return null;
        }


        @Override
        public void save(User arg0) throws FtpException
        {
        }
    }
}
