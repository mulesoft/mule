/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.payload;


import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.mule.module.cxf.payload.PayloadTestConstants.emptyOjbectArrayPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.greetMeOutEndpointName;
import static org.mule.module.cxf.payload.PayloadTestConstants.nullPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.objectPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.sayHiOutEndpointName;
import static org.mule.module.cxf.payload.PayloadTestConstants.strArrayPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.strArrayPayloadResult;
import static org.mule.module.cxf.payload.PayloadTestConstants.strPayload;
import static org.mule.module.cxf.payload.PayloadTestConstants.strPayloadResult;

/**
 * This tests the payloadToArguments attribute on the cxf outbound endpoints for the
 * case it is supplied with value nullPayloadAsVoid.
 */
public class TreatNullPayloadAsVoidTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
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
