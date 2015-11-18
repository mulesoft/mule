/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.connector;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreClient;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStorePoolingProfile;

import java.util.Collection;

import org.junit.runners.Parameterized;

public class PetStorePoolableConnectionTestCase extends PetStorePooledConnectionTestCase
{

    private static final String NO_POOLING = "noPooling";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {
                {"petstore", PetStorePoolingProfile.MAX_ACTIVE},
                {"customPooling", PetStorePoolingProfile.MAX_ACTIVE + 1},
                {NO_POOLING, 0}});
    }


    public PetStorePoolableConnectionTestCase(String name, int poolSize)
    {
        super(name, poolSize);
    }

    @Override
    protected String getConfigFile()
    {
        return "petstore-poolable-connection.xml";
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
