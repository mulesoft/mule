/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.messagedispatcher;

import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.emptyOjbectArrayPayload;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.greetMeOutEndpointName;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.nullPayload;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.objectPayload;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.sayHiOutEndpointName;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.strArrayPayload;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.strArrayPayloadResult;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.strPayload;
import static org.mule.transport.cxf.messagedispatcher.CxfMessageDispatcherTestConstants.strPayloadResult;

import org.mule.tck.FunctionalTestCase;

/**
 * This tests the payloadToArguments attribute on the cxf outbound endpoints for the
 * default case (when it is not supplied).
 */
public class CxfMessageDispatcherTreatNullPayloadAsParameterByDefaultTestCase extends FunctionalTestCase
{
    public void testRunAllScenarios() throws Exception
    {
        CallAndExpect[] callAndExpectArray = {
            new CallAndExpectArgumentTypeMismatch(greetMeOutEndpointName, nullPayload, muleContext),
            new CallAndExpectArgumentTypeMismatch(greetMeOutEndpointName, objectPayload, muleContext),
            new CallAndExpectPayloadResult(greetMeOutEndpointName, strPayload, strPayloadResult, muleContext),
            new CallAndExpectPayloadResult(greetMeOutEndpointName, strArrayPayload, strArrayPayloadResult,
                muleContext),
            new CallAndExpectWrongNumberOfArguments(greetMeOutEndpointName, emptyOjbectArrayPayload,
                muleContext),

            new CallAndExpectWrongNumberOfArguments(sayHiOutEndpointName, nullPayload, muleContext),
            new CallAndExpectWrongNumberOfArguments(sayHiOutEndpointName, objectPayload, muleContext),
            new CallAndExpectWrongNumberOfArguments(sayHiOutEndpointName, strPayload, muleContext),
            new CallAndExpectWrongNumberOfArguments(sayHiOutEndpointName, strArrayPayload, muleContext),
            new CallAndExpectPayloadResult(sayHiOutEndpointName, emptyOjbectArrayPayload, "Bonjour",
                muleContext)};

        for (CallAndExpect callAndExpect : callAndExpectArray)
        {
            callAndExpect.callEndpointAndExecuteAsserts();
        }
    }

    @Override
    protected String getConfigResources()
    {
        return "messagedispatcher/null-payload-add-as-parameter-by-default.xml";
    }
}
