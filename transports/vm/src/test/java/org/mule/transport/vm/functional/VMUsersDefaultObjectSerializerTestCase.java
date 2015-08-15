/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.serialization.internal.JavaObjectSerializer;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class VMUsersDefaultObjectSerializerTestCase extends FunctionalTestCase
{

    private ObjectSerializer objectSerializer;

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        super.addBuilders(builders);

        objectSerializer = new JavaObjectSerializer();
        try
        {
            LifecycleUtils.initialiseIfNeeded(objectSerializer, muleContext);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        objectSerializer = spy(objectSerializer);

        Map<String, Object> serializerMap = new HashMap<>();
        serializerMap.put("customSerializer", objectSerializer);
        builders.add(0, new SimpleConfigurationBuilder(serializerMap));
    }

    @Override
    protected String getConfigFile()
    {
        return "vm/vm-uses-default-object-serializer-test-flow.xml";
    }

    @Test
    public void serializeWithKryo() throws Exception
    {
        final String payload = "payload";
        runFlow("dispatch", getTestEvent(payload));

        MuleMessage response = muleContext.getClient().request("vm://in", 5000);
        assertThat(response, is(notNullValue()));
        assertThat(response.getPayloadAsString(), is(payload));

        ArgumentCaptor<MuleMessage> messageArgumentCaptor = ArgumentCaptor.forClass(MuleMessage.class);
        verify(objectSerializer, atLeastOnce()).serialize(messageArgumentCaptor.capture());
        MuleMessage capturedMessage = messageArgumentCaptor.getValue();
        assertThat(capturedMessage, is(notNullValue()));
        assertThat(capturedMessage.getPayloadAsString(), is(payload));

        verify(objectSerializer, atLeastOnce()).deserialize(any(byte[].class));
    }

}
