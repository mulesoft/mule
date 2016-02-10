/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.connector;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MessagingException;
import org.mule.api.connection.ConnectionException;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreClient;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStorePoolingProfile;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.util.concurrent.Latch;

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
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PetStorePooledConnectionTestCase extends PetStoreConnectionTestCase
{

    private ExecutorService executorService = null;
    private Latch connectionLatch = new Latch();
    private CountDownLatch testLatch;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {
                {"petstore", PetStorePoolingProfile.MAX_ACTIVE},
                {"customPooling", PetStorePoolingProfile.MAX_ACTIVE + 1}});
    }

    @Rule
    public SystemProperty configNameProperty;

    protected final String name;
    private final int poolSize;

    public PetStorePooledConnectionTestCase(String name, int poolSize)
    {
        this.name = name;
        configNameProperty = new SystemProperty("configName", name);
        this.poolSize = poolSize;

        testLatch = new CountDownLatch(poolSize);
    }

    @Override
    protected String getConfigFile()
    {
        return "petstore-pooled-connection.xml";
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
        if (poolSize == 0)
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
            fail("was expecting pool to be exhausted");
        }
        catch (MessagingException e)
        {
            assertThat(e.getCauseException(), is(instanceOf(ConnectionException.class)));
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
        return executorService.submit(() -> {
            return (PetStoreClient) flowRunner("getClientOnLatch").withPayload("")
                                                                  .withFlowVariable("testLatch", testLatch)
                                                                  .withFlowVariable("connectionLatch", connectionLatch)
                                                                  .run()
                                                                  .getMessage()
                                                                  .getPayload();
        });
    }
}
