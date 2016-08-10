/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.payload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.module.cxf.CxfOutboundMessageProcessor;

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

    @Override
    public void callEndpointAndExecuteAsserts() throws MuleException
    {
        MuleClient client = muleContext.getClient();
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
