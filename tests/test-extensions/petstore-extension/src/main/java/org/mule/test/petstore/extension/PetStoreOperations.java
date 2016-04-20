/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PetStoreOperations
{

    public List<String> getPets(@Connection PetStoreClient client, @UseConfig PetStoreConnector config, String ownerName)
    {
        return client.getPets(ownerName, config);
    }

    public PetStoreClient getClient(@Connection PetStoreClient client)
    {
        return client;
    }

    public PetStoreClient getClientOnLatch(@Connection PetStoreClient client, MuleEvent event) throws Exception
    {
        CountDownLatch countDownLatch = event.getFlowVariable("testLatch");
        if (countDownLatch != null)
        {
            countDownLatch.countDown();
        }

        Latch latch = event.getFlowVariable("connectionLatch");
        latch.await();
        return client;
    }
}
