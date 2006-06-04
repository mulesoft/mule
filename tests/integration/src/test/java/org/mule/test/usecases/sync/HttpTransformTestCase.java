/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformers.compression.GZipUncompressTransformer;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.umo.UMOMessage;

import java.util.Arrays;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class HttpTransformTestCase extends FunctionalTestCase {

    protected String getConfigResources() {
        return "org/mule/test/usecases/sync/http-transform.xml";
    }

    public void testTransform() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("http://localhost:18080/RemoteService", "payload", null);
        assertNotNull(message);
        GZipUncompressTransformer gu = new GZipUncompressTransformer();
        gu.setReturnClass(String.class);
        assertNotNull(message.getPayload());
        String result = (String) gu.transform(message.getPayloadAsBytes());
        assertEquals("<string>payload</string>", result);
    }

    public void testBinary() throws Exception
    {
        MuleClient client = new MuleClient();
        Object payload = Arrays.asList(new Integer[] {new Integer(42)});
        UMOMessage message = client.send("http://localhost:18081/RemoteService", payload, null);
        assertNotNull(message);
        ByteArrayToSerializable bas = new ByteArrayToSerializable();
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof byte[]);
        Object result = bas.transform(message.getPayload());
        assertEquals(payload, result);
    }

    public void testBinaryWithBridge() throws Exception
    {
        MuleClient client = new MuleClient();
        Object payload = Arrays.asList(new Integer[] {new Integer(42)});
        UMOMessage message = client.send("vm://LocalService", payload, null);
        assertNotNull(message);
        ByteArrayToSerializable bas = new ByteArrayToSerializable();
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof byte[]);
        Object result = bas.transform(message.getPayload());
        assertEquals(payload, result);
    }
}
