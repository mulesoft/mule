/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.Rule;
import org.junit.Test;

public class FtpReconnectionNotifierTestCase extends FunctionalTestCase
{

    private final static Integer RECONNECTION_ATTEMPTS = 2;
    private final static Integer POLLING_FREQUENCY = 100;

    @Rule
    public SystemProperty countSystemProperty = new SystemProperty("count", RECONNECTION_ATTEMPTS.toString());

    @Rule
    public SystemProperty pollingFrequencySystemProperty = new SystemProperty("pollingFrequency", POLLING_FREQUENCY.toString());

    @Rule
    public DynamicPort ftpPort = new DynamicPort("port");

    private TestReconnectNotifier notifier = null;

    @Override
    protected String getConfigFile()
    {
        return "ftp-reconnection-notifier-fails-config.xml";
    }

    @Test
    public void testNotificationOnReconnectionAttempts() throws Exception
    {
        notifier = mock(TestReconnectNotifier.class);
        Connector c = muleContext.getRegistry().lookupConnector("FTP");
        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        rpf.setNotifier(notifier);
        MuleFtpServer server = new MuleFtpServer(ftpPort.getNumber(), 1);
        server.start();
        serverConnected();
        server.stop();
        serverDisconnected();
    }

    private void verifyOnSuccessNotification()
    {
        PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, POLLING_FREQUENCY);
        pollingProber.check(new JUnitProbe()
        {
            @Override
            protected boolean test()
            {
                verify(notifier).onSuccess(any(RetryContext.class));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Success notification wasn't triggered";
            }
        });
    }

    private void verifyOnFailureNotification()
    {
        PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, POLLING_FREQUENCY);
        pollingProber.check(new JUnitProbe()
        {
            @Override
            protected boolean test()
            {
                verify(notifier, times(3)).onFailure(any(RetryContext.class), any(Throwable.class));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Failure notification wasn't triggered";
            }
        });
    }

    private void serverConnected() throws Exception
    {
        runFlow("main-test");
        verifyOnSuccessNotification();
    }

    private void serverDisconnected()
    {
        try
        {
            runFlow("main-test");
            fail("ConnectException wasn't caught.");
        }
        catch (Exception e)
        {
            verifyOnFailureNotification();
        }
    }

    public static class MuleFtpServer
    {

        private final FtpServer server;
        private final FtpServerFactory serverFactory;
        private final ListenerFactory factory;

        public MuleFtpServer(int port, long delay)
        {
            serverFactory = new FtpServerFactory();
            factory = new ListenerFactory();
            factory.setPort(port);
            serverFactory.addListener("default", factory.createListener());
            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            serverFactory.setUserManager(userManagerFactory.createUserManager());
            BaseUser user = new BaseUser();
            user.setName("mule-test");
            user.setPassword("mule-test");
            List<Authority> authorities = new ArrayList<Authority>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);
            try
            {
                serverFactory.getUserManager().save(user);
            }
            catch (FtpException e)
            {
                throw new RuntimeException(e);
            }
            server = serverFactory.createServer();
        }

        public void start()
        {
            try
            {
                server.start();
            }
            catch (FtpException e)
            {
                throw new RuntimeException(e);
            }
        }

        public void stop()
        {
            try
            {
                server.stop();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestReconnectNotifier implements RetryNotifier
    {

        public static volatile int fails = 0;
        public static volatile int successes = 0;

        @Override
        public void onFailure(RetryContext retryContext, Throwable throwable)
        {
            fails++;
        }

        @Override
        public void onSuccess(RetryContext retryContext)
        {
            successes++;
        }

    }
}

