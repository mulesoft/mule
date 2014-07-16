/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;

public class HttpContentLengthPropagationTestCase extends FunctionalTestCase
{
    private static final String NAME_PAYLOAD = "test-xml-payload.xml";
    private static final String NAME_STYLESHEET = "stylesheet.xsl";

    @Override
    protected String getConfigFile()
    {
        return "http-content-length-propagation-conf.xml";
    }

    @Test
    public void testContentLengthPropagation() throws Exception
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(NAME_PAYLOAD);
        assertNotNull("Payload test file not found.", is);
        byte[] fileContents = IOUtils.toByteArray(is);

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send(((InboundEndpoint) muleContext.getRegistry().lookupObject("httpEndpoint")).getAddress(),
            new DefaultMuleMessage(fileContents, muleContext));

        XsltTransformer trans = new XsltTransformer();
        trans.setMuleContext(muleContext);
        trans.setXslFile(NAME_STYLESHEET);
        trans.initialise();
        final byte[] locallyTransformedBytes = (byte[]) trans.doTransform(fileContents, "UTF-8");

        assertEquals(new String(locallyTransformedBytes), result.getPayloadAsString());
    }
}
