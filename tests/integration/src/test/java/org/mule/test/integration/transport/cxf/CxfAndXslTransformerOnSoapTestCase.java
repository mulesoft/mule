/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.support.OutputPayloadInterceptor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.atomic.AtomicInteger;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class CxfAndXslTransformerOnSoapTestCase extends FunctionalTestCase
{
    private static final String msg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:emop=\"http://www.wcs.com/2010/07/14/emop\">"
                       + "  <soapenv:Header>\n"
                       + "    <header UserName=\"nothing\" Password=\"important\"/>\n"
                       + "  </soapenv:Header>\n"
                       + "  <soapenv:Body>\n"
                       + "    <emop:ScratchcardValidateAndPayRequestBody>\n"
                       + "       <ScratchcardNumber>1</ScratchcardNumber>\n"
                       + "       <VirnNumber>2</VirnNumber>\n"
                       + "    </emop:ScratchcardValidateAndPayRequestBody>\n"
                       + "  </soapenv:Body>\n"
                       + "</soapenv:Envelope>";

    private final AtomicInteger connectorExceptionCounter = new AtomicInteger();

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        connectorExceptionCounter.set(0);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/cxf/scratchcard-service-v1.xml";
    }

    /**
     * This test fails without the change involving the
     * {@link OutputPayloadInterceptor#cleanUpPayload(Object)}. It is a fix for issue
     * MULE-5030.
     * 
     * @throws Exception
     */
    @Test
    public void testUsesTransformersCorrectly() throws Exception
    {
        TestExceptionStrategy exceptionStrategy = new TestExceptionStrategy();
        muleContext.setExceptionListener(exceptionStrategy);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:28181/ScratchCardServiceV1", msg, null);
        assertNotNull("The result shouln't have been null", result);
        final String payloadAsString = result.getPayloadAsString();
        assertNotNull("The payloadAsString shouln't have been null", payloadAsString);
        assertFalse("There shouldn't be a fault in the payload: " + payloadAsString,
            payloadAsString.contains("<soap:Fault>"));

        final Latch latch = new Latch();
        exceptionStrategy.setExceptionCallback(new ExceptionCallback()
        {
            public void onException(Throwable t)
            {
                connectorExceptionCounter.incrementAndGet();
                latch.countDown();
            }
        });

        latch.await(500, TimeUnit.MILLISECONDS);
        assertEquals("There shouldn't have been any exceptions", 0, connectorExceptionCounter.get());
    }
}


