/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import java.util.Map;

public interface SecurityStrategy
{

    /**
     * Applies this security strategy to inbound and outbound configuration maps.
     *
     * @param outConfigProperties Properties to be set on the out interceptor (applied to the SOAP request).
     * @param inConfigProperties Properties to be set on the in interceptor (applied to the SOAP response).
     */
    public void apply(Map<String, Object> outConfigProperties, Map<String, Object> inConfigProperties);

}
