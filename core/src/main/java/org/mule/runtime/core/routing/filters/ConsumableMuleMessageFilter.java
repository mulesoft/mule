/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import static org.mule.util.ClassUtils.isConsumable;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

/**
 * Filters messages that have a consumable payload.
 * <p/>
 * The filter accepts only {@link DefaultMuleMessage} instances that
 * have a no consumable payload. Check is done using
 * {@link org.mule.DefaultMuleMessage#isConsumable()} method.
 */
public class ConsumableMuleMessageFilter implements Filter
{

    @Override
    public boolean accept(MuleMessage message)
    {

        if (message instanceof DefaultMuleMessage)
        {
            return !isConsumable(message.getPayload().getClass());
        }
        else
        {
            return false;
        }
    }
}
