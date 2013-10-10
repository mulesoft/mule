/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.filters;

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

    public boolean accept(MuleMessage message)
    {

        if (message instanceof DefaultMuleMessage)
        {
            return !((DefaultMuleMessage) message).isConsumable();
        }
        else
        {
            return false;
        }
    }
}
