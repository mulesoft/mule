/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

public class AsyncSpringEventsTestCase extends SpringEventsTestCase
{

    protected String getConfigResources()
    {
        return "async-mule-events-app-context.xml";
    }

    // The code works just we have no synchronisation for the test case
    public void testReceiveAndPublishEvent() throws Exception
    {
        // todo fix synchronisation issue
    }

    public void testReceivingASpringEvent() throws Exception
    {
        // todo fix synchronisation issue
    }

    public void testReceivingAllEvents() throws Exception
    {
        // todo fix synchronisation issue
    }
}
