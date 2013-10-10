/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.support.OutputPayloadInterceptor;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class CxfAndXslTransformerOnSoapTestCase extends AbstractServiceAndFlowTestCase
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

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/transport/cxf/scratchcard-service-v1-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/transport/cxf/scratchcard-service-v1-flow.xml"}});
    }

    public CxfAndXslTransformerOnSoapTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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
