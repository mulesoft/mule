/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.payload;

import org.mule.api.MuleContext;

/**
 *
 */
class CallAndExpectWrongNumberOfArguments extends AbstractCallAndExpectIllegalArgumentException
{
    public CallAndExpectWrongNumberOfArguments(String outputEndpointName,
                                               Object payload,
                                               MuleContext muleContext)
    {
        super(outputEndpointName, payload, muleContext);
    }

    @Override
    public String expectedIllegalArgumentExceptionMessage()
    {
        return "wrong number of arguments";
    }
}
