/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    //There is no transformer to go from String to StringBuffer
    public Object processFailedAutoTransformString(@Payload StringBuffer payload)
    {
        return payload;
    }
}
