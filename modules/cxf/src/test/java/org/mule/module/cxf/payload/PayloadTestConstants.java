/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


