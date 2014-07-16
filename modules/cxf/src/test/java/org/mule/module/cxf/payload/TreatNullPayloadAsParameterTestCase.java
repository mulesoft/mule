/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
    {
        return "messagedispatcher/null-payload-add-as-parameter.xml";
    }
}
