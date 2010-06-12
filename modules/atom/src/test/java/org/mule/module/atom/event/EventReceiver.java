/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.event;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import org.apache.abdera.model.Feed;

public class EventReceiver implements Callable
{

    public static int receivedEntries = 0;

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        Feed feed = eventContext.getMessage().getPayload(Feed.class);
        System.out.println("Received " + feed.getEntries().size() + " events");

        receivedEntries = feed.getEntries().size();
        return null;
    }
}
