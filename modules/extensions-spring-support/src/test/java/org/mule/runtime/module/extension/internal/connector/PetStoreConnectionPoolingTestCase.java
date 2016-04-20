/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PetStoreConnectionPoolingTestCase extends PetStoreConnectionTestCase
{

    private static final String DEFAULT_POOLING_CONFIG = "defaultPoolingPoolable";
    private static final String CUSTOM_POOLING_CONFIG = "customPooling";
    private static final String CUSTOM_POOLING_POOLED_CONFIG = CUSTOM_POOLING_CONFIG + "Pooled";
    private static final String CUSTOM_POOLING_POOLABLE_CONFIG = CUSTOM_POOLING_CONFIG + "Poolable";
    private static final String NO_POOLING = "noPooling";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {
                {CUSTOM_POOLING_POOLABLE_CONFIG, 3},
                {CUSTOM_POOLING_POOLED_CONFIG, 3},
                {NO_POOLING, 0}});
    }

    @Rule
    public SystemProperty configNameProperty;

    private ExecutorService executorService = null;
    private Latch connectionLatch = new Latch();
    private CountDownLatch testLatch;

    protected int poolSize;
    protected String name;

    public PetStoreConnectionPoolingTestCase(String name, int poolSize)
    {
        this.name = name;
        this.poolSize = poolSize;
        configNameProperty = new SystemProperty("configName", name);
        testLatch = new CountDownLatch(poolSize);
    }

    @Override
    protected String getConfigFile()
    {
        return "petstore-pooling-connection.xml";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (executorService != null)
        {
            executorService.shutdown();
        }
    }

    @Test
    public void exhaustion() throws Exception
    {
        if (NO_POOLING.equals(name))
        {
            // test does not apply
            return;
        }

        executorService = Executors.newFixedThreadPool(poolSize);

        List<Future<PetStoreClient>> clients = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++)
        {
            clients.add(getClientOnLatch());
        }

        testLatch.await();

        try
        {
            getClient();
            fail("was expecting pool to be exhausted when using config: " + name);
        }
        catch (MessagingException e)
        {
            assertThat(e.getCauseException(), is(instanceOf(ConnectionException.class)));
        }
        catch (Exception e)
        {
            fail("a connection exception was expected");
        }

        connectionLatch.release();

        for (Future<PetStoreClient> future : clients)
        {
            PollingProber prober = new PollingProber(1000, 100);
            prober.check(new JUnitProbe()
            {
                @Override
                protected boolean test() throws Exception
                {
                    PetStoreClient client = future.get(100, MILLISECONDS);
                    assertValidClient(client);
                    return true;
                }

                @Override
                public String describeFailure()
                {
                    return "Could not obtain valid client";
                }
            });
        }

        //now test that the pool is usable again
        assertValidClient(getClient());
    }

    protected Future<PetStoreClient> getClientOnLatch()
    {
        return executorService.submit(() -> (PetStoreClient) flowRunner("getClientOnLatch").withPayload("")
                                                              .withFlowVariable("testLatch", testLatch)
                                                              .withFlowVariable("connectionLatch", connectionLatch)
                                                              .run()
                                                              .getMessage()
                                                              .getPayload());
    }

    @Override
    protected void assertConnected(PetStoreClient client)
    {
        if (NO_POOLING.equals(name))
        {
            assertThat(client.isConnected(), is(false));
        }
        else
        {
            super.assertConnected(client);
        }
    }

}
