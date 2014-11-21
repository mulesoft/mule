/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.HttpConstants.Methods.POST;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.cxf.support.OutputPayloadInterceptor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

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
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transport/cxf/scratchcard-service-v1-flow.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        connectorExceptionCounter.set(0);
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

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:28181/ScratchCardServiceV1", new DefaultMuleMessage(msg, muleContext),
                                         HttpRequestOptionsBuilder.newOptions().method(POST.name()).disableStatusCodeValidation().build());
        assertNotNull("The result shouln't have been null", result);
        final String payloadAsString = result.getPayloadAsString();
        assertNotNull("The payloadAsString shouln't have been null", payloadAsString);
        assertFalse("There shouldn't be a fault in the payload: " + payloadAsString,
            payloadAsString.contains("<soap:Fault>"));

        final Latch latch = new Latch();
        exceptionStrategy.setExceptionCallback(new ExceptionCallback()
        {
            @Override
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
