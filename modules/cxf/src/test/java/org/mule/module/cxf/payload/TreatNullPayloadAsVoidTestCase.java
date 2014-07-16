/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.payload;

import static org.mule.module.cxf.payload.PayloadTestConstants.emptyOjbectArrayPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.greetMeOutEndpointName;
import static org.mule.module.cxf.payload.PayloadTestConstants.nullPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.objectPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.sayHiOutEndpointName;
import static org.mule.module.cxf.payload.PayloadTestConstants.strArrayPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.strArrayPayloadResult;
import static org.mule.module.cxf.payload.PayloadTestConstants.strPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.strPayloadResult;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

/**
 * This tests the payloadToArguments attribute on the cxf outbound endpoints for the
 * case it is supplied with value nullPayloadAsVoid.
 */
public class TreatNullPayloadAsVoidTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "messagedispatcher/null-payload-add-as-void.xml";
    }

    @Test
    public void testRunAllScenarios() throws Exception
    {
        CallAndExpect[] callAndExpectArray = {
            new CallAndExpectWrongNumberOfArguments(greetMeOutEndpointName, nullPayload, muleContext),
            new CallAndExpectArgumentTypeMismatch(greetMeOutEndpointName, objectPayload, muleContext),
            new CallAndExpectPayloadResult(greetMeOutEndpointName, strPayload, strPayloadResult, muleContext),
            new CallAndExpectPayloadResult(greetMeOutEndpointName, strArrayPayload, strArrayPayloadResult,
                muleContext),
            new CallAndExpectWrongNumberOfArguments(greetMeOutEndpointName, emptyOjbectArrayPayload,
                muleContext),

            new CallAndExpectPayloadResult(sayHiOutEndpointName, nullPayload, "Bonjour", muleContext),
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
}
