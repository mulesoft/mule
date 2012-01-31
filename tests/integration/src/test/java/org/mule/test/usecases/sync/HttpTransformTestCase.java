/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transformer.compression.GZipUncompressTransformer;
import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.SerializationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpTransformTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/usecases/sync/http-transform-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/usecases/sync/http-transform-flow.xml"}
        });
    }

    public HttpTransformTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testTransform() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("http://localhost:18080/RemoteService", "payload", null);
        assertNotNull(message);
        GZipUncompressTransformer gu = new GZipUncompressTransformer();
        gu.setMuleContext(muleContext);
        gu.setReturnDataType(DataTypeFactory.STRING);
        assertNotNull(message.getPayload());
        String result = (String)gu.transform(message.getPayloadAsBytes());
        assertEquals("<string>payload</string>", result);
    }

    @Test
    public void testBinary() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        ArrayList<Integer> payload = new ArrayList();
        payload.add(42);
        MuleMessage message = client.send("http://localhost:18081/RemoteService", SerializationUtils.serialize(payload), null);
        assertNotNull(message);
        ByteArrayToSerializable bas = new ByteArrayToSerializable();
        bas.setMuleContext(muleContext);
        assertNotNull(message.getPayload());
        Object result = bas.transform(message.getPayload());
        assertEquals(payload, result);
    }

    @Test
    public void testBinaryWithBridge() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Object payload = Arrays.asList(42);
        MuleMessage message = client.send("vm://LocalService", payload, null);
        assertNotNull(message);
        ByteArrayToSerializable bas = new ByteArrayToSerializable();
        bas.setMuleContext(muleContext);
        assertNotNull(message.getPayload());
        Object result = bas.transform(message.getPayload());
        assertEquals(payload, result);
    }
}
