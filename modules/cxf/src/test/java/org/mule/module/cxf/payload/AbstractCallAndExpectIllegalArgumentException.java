/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.payload;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.CxfOutboundMessageProcessor;

import junit.framework.TestCase;

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
            TestCase.fail(here() + " should have thrown an exception");
        }
        catch (MuleException e)
        {
            e.printStackTrace();
            TestCase.assertTrue(here() + ", exception {" + e + "} must be a "
                       + DispatchException.class.getSimpleName(), e instanceof DispatchException);
            TestCase.assertTrue(here() + ", exception.getCause() {" + e + "} must be a "
                       + IllegalArgumentException.class.getName(),
                e.getCause() instanceof IllegalArgumentException);
            TestCase.assertEquals(here(), expectedIllegalArgumentExceptionMessage(), e.getCause()
                .getMessage());
        }
    }

    private String here()
    {
        return "In [" + outputEndpointName + "," + payload + "]";
    }

    public abstract String expectedIllegalArgumentExceptionMessage();
}
