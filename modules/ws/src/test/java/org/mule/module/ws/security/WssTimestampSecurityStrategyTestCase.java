/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import static org.apache.ws.security.handler.WSHandlerConstants.ACTION;
import static org.apache.ws.security.handler.WSHandlerConstants.TIMESTAMP;
import static org.apache.ws.security.handler.WSHandlerConstants.TTL_TIMESTAMP;
import static org.apache.ws.security.handler.WSHandlerConstants.USERNAME_TOKEN;
import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class WssTimestampSecurityStrategyTestCase extends AbstractMuleTestCase
{
    private static final long EXPIRES = 30;

    private WssTimestampSecurityStrategy strategy = new WssTimestampSecurityStrategy();

    @Test
    public void actionAndTtlTimestampFieldsAreSetOnEmptyMap()
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        strategy.setExpires(EXPIRES);
        strategy.apply(properties, null);

        assertEquals(TIMESTAMP, properties.get(ACTION));
        assertEquals(String.valueOf(EXPIRES), properties.get(TTL_TIMESTAMP));
    }

    @Test
    public void actionIsAppendedAfterExistingAction()
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ACTION, USERNAME_TOKEN);

        strategy.setExpires(EXPIRES);
        strategy.apply(properties, null);

        String expectedAction = USERNAME_TOKEN + " " + TIMESTAMP;
        assertEquals(expectedAction, properties.get(ACTION));
        assertEquals(String.valueOf(EXPIRES), properties.get(TTL_TIMESTAMP));
    }


}
