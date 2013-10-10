/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
