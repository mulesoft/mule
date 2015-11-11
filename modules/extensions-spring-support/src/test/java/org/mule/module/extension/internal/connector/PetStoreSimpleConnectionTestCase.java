/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.connector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreClient;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreConnector;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class PetStoreSimpleConnectionTestCase extends ExtensionsFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "petstore-simple-connection.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {PetStoreConnector.class};
    }

    @Test
    public void getPets() throws Exception
    {
        ConfigurationInstance<PetStoreConnector> config = muleContext.getExtensionManager().getConfiguration("petstore", getTestEvent(""));
        assertThat(config, is(notNullValue()));

        MuleEvent response = runFlow("getPets");
        List<String> pets = (List<String>) response.getMessage().getPayload();
        assertThat(pets, containsInAnyOrder(config.getValue().getPets().toArray()));
    }

    @Test
    public void verifyClient() throws Exception
    {
        PetStoreClient client = getClient();
        assertThat(client.getUsername(), is("john"));
        assertThat(client.getPassword(), is("doe"));

        assertThat(client.isConnected(), is(true));
    }

    @Test
    @Ignore("MULE-8952: Reenable after MULE-8952")
    public void stopAndDisconnect() throws Exception
    {
        PetStoreClient client = getClient();
        muleContext.stop();

        assertThat(client.getDisconnectCount(), is(1));
        assertThat(client.isConnected(), is(false));
    }

    private PetStoreClient getClient() throws Exception
    {
        PetStoreClient client = (PetStoreClient) runFlow("getClient").getMessage().getPayload();
        assertThat(client, is(notNullValue()));
        return client;
    }
}
