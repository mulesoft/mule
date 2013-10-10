/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.payload;


/**
 * This tests the payloadToArguments attribute on the cxf outbound endpoints for the
 * case it is supplied with value nullPayloadAsParameter.
 */
public class TreatNullPayloadAsParameterTestCase extends
    TreatNullPayloadAsParameterByDefaultTestCase
{


    @Override
    protected String getConfigResources()
    {
        return "messagedispatcher/null-payload-add-as-parameter.xml";
    }
}
