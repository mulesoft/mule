/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.payload;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.CxfOutboundMessageProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This is an abstract utility class that helps the testing of
 * {@link CxfOutboundMessageProcessor} on classes
 * {@link TreatNullPayloadAsParameterByDefaultTestCase},
 * {@link TreatNullPayloadAsParameterTestCase} and
 * {@link TreatNullPayloadAsVoidTestCase}.
 */
abstract class AbstractCallAndExpectIllegalArgumentException implements CallAndExpect
{
    private final String outputEndpointName;
    private final Object payload;
    private final MuleContext muleContext;

    public AbstractCallAndExpectIllegalArgumentException(String outputEndpointName,
                                                         Object payload,
                                                         MuleContext muleContext)
    {
        this.outputEndpointName = outputEndpointName;
        this.payload = payload;
        this.muleContext = muleContext;
    }

    public void callEndpointAndExecuteAsserts() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send(outputEndpointName, payload, null);
            fail(here() + " should have thrown an exception");
        }
        catch (MuleException e)
        {
            e.printStackTrace();
            assertTrue(here() + ", exception {" + e + "} must be a "
                       + DispatchException.class.getSimpleName(), e instanceof DispatchException);
            assertTrue(here() + ", exception.getCause() {" + e + "} must be a "
                       + IllegalArgumentException.class.getName(),
                       e.getCause() instanceof IllegalArgumentException);
            assertEquals(here(), expectedIllegalArgumentExceptionMessage(), e.getCause()
                    .getMessage());
        }
    }

    private String here()
    {
        return "In [" + outputEndpointName + "," + payload + "]";
    }

    public abstract String expectedIllegalArgumentExceptionMessage();
}
