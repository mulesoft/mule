/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMessageCollection;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.routing.RouterResultsHandler;

import java.util.Iterator;
import java.util.List;

/**
 * The default results handler for all outbound endpoint. Depending on the number of messages passed it
 * the returning message will be different.
 * If the 'results' param is null or empty, null is returned.
 * If the 'results' param contains a single {@link org.mule.api.MuleMessage}, than that message is returned.
 * If the 'results' param contains more than one message a {@link org.mule.api.MuleMessageCollection} instance
 * is returned.
 * <p/>
 * Note that right now (as of Mule 2.0.1) this SPI is not pluggable and this implementation is the default and
 * only implementation.
 *
 * @see org.mule.api.MuleMessageCollection
 * @see org.mule.api.MuleMessage
 * @see org.mule.DefaultMessageCollection
 */
public class DefaultRouterResultsHandler implements RouterResultsHandler
{
    public MuleMessage aggregateResults(List /*<MuleMessage>*/ results, MuleMessage orginalMessage)
    {
        if (results == null || results.size() == 0)
        {
            return null;
        }
        else if (results.size() == 1)
        {
            return (MuleMessage) results.get(0);
        }
        else
        {
            MuleMessageCollection coll = new DefaultMessageCollection();
            for (Iterator iterator = results.iterator(); iterator.hasNext();)
            {
                MuleMessage muleMessage = (MuleMessage) iterator.next();
                coll.addMessage(muleMessage);
            }
            return coll;
        }
    }
}
