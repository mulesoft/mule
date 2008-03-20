/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class InterceptorAdapterTestCase extends FunctionalTestCase
{

    public static final String MESSAGE = "squeak";

    protected String getConfigResources()
    {
        return "org/mule/config/spring/interceptor-adapter-test.xml";
    }

    public void testInterceptor() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("vm://in", MESSAGE, null);
        assertEquals(MESSAGE, response.getPayloadAsString());
        TimingInterceptor timer = (TimingInterceptor) muleContext.getRegistry().lookupObject("timer");
        assertNotNull(timer);
        long ms = timer.getInterval();
        assertTrue(TimingInterceptor.UNCALLED != ms);
    }

}