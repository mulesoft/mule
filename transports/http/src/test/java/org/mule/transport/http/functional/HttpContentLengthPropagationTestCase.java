/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HttpContentLengthPropagationTestCase extends FunctionalTestCase
{

    private static final String NAME_PAYLOAD = "test-xml-payload.xml";
    private static final String NAME_STYLESHEET = "stylesheet.xsl";

    @Override
    protected String getConfigResources()
    {
        return "http-content-length-propagation-conf.xml";
    }

    @Test
    public void testContentLengthPropagation() throws Exception
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(NAME_PAYLOAD);
        assertNotNull("Payload test file not found.", is);
        byte[] fileContents = IOUtils.toByteArray(is);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("httpEndpoint")).getAddress(),
            new DefaultMuleMessage(fileContents, muleContext));

        XsltTransformer trans = new XsltTransformer();
        trans.setMuleContext(muleContext);
        trans.setXslFile(NAME_STYLESHEET);
        trans.initialise();
        final byte[] locallyTransformedBytes = (byte[]) trans.doTransform(fileContents, "UTF-8");

        assertEquals(new String(locallyTransformedBytes), result.getPayloadAsString());
    }
}
