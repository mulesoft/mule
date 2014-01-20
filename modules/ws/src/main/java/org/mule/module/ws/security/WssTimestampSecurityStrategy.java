/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import java.util.Map;

import org.apache.ws.security.handler.WSHandlerConstants;

public class WssTimestampSecurityStrategy extends AbstractSecurityStrategy implements SecurityStrategy
{
    private long expires;

    @Override
    public void apply(Map<String, Object> configProperties)
    {
        appendAction(configProperties, WSHandlerConstants.TIMESTAMP);
        configProperties.put(WSHandlerConstants.TTL_TIMESTAMP, String.valueOf(expires));
    }

    public long getExpires()
    {
        return expires;
    }

    public void setExpires(long expires)
    {
        this.expires = expires;
    }
}
