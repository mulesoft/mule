/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import java.io.InputStream;

/**
 * Test cases where the Payload annotation can be used to specify the parameter to inject the payload of the message, including
 * doing automatic transforms
 */
public class PayloadAnnotationComponent
{
    //No transform needed
    public String processNoTransformString(@Payload String payload)
    {
        return payload;
    }

    //Auto transform from String to InputStream
    public InputStream processAutoTransformString(@Payload InputStream payload)
    {
        return payload;
    }

    //There is no transformer to go from String to StringBuilder
    public Object processFailedAutoTransformString(@Payload StringBuilder payload)
    {
        return payload;
    }
}
