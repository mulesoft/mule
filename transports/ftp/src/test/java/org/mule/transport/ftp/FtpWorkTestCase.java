/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.util.lock.LockFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class FtpWorkTestCase extends AbstractFtpServerTestCase
{
    DefaultRegistryBroker registryBroker = mock(DefaultRegistryBroker.class);
    MockLock lock;

    public FtpWorkTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "ftp-work.xml"}
        });
    }

    @Override
    public void doSetUp() throws Exception
    {
        super.doSetUp();
        lock = new MockLock();

        when(registryBroker.get(MuleProperties.OBJECT_LOCK_FACTORY)).thenReturn(new LockFactory()
        {
            @Override
            public Lock createLock(String lockId)
            {
                return lock;
            }
        });
        when(registryBroker.lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY)).thenReturn(muleContext.getEndpointFactory());

        ((DefaultMuleContext)muleContext).setRegistryBroker(registryBroker);
    }

    @Test
    public void testRetryWork() throws Exception
    {
        File tmpDir = getFtpServerBaseDir();
        createDataFile(tmpDir, TEST_MESSAGE);

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);

        assertEquals(2, lock.attemps);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }

    private class MockLock implements Lock
    {
        int attemps = 0;

        @Override
        public void lock()
        {

        }

        @Override
        public void lockInterruptibly() throws InterruptedException
        {

        }

        @Override
        public boolean tryLock()
        {
            attemps++;
            return attemps > 1;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
        {
            return false;
        }

        @Override
        public void unlock()
        {

        }

        @Override
        public Condition newCondition()
        {
            return null;
        }
    }
}
