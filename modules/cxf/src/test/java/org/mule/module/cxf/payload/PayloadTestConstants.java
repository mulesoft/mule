/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.payload;

import org.mule.transport.NullPayload;

public interface PayloadTestConstants
{
    public static final NullPayload nullPayload = NullPayload.getInstance();
    public static final Object objectPayload = new Object();
    public static final String strPayload = "some message";
    public static final String[] strArrayPayload = new String[]{"some message"};
    public static final Object[] emptyOjbectArrayPayload = new Object[]{};

    public static final String strPayloadResult = "Hello " + strPayload;
    public static final String strArrayPayloadResult = "Hello " + strArrayPayload[0];

    public static final String greetMeOutEndpointName = "greetMeOutboundEndpoint";
    public static final String sayHiOutEndpointName = "sayHiOutboundEndpoint";
}


