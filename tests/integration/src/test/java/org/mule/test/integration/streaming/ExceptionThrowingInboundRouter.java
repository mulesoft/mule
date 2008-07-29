/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.streaming;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.routing.inbound.SelectiveConsumer;

public class ExceptionThrowingInboundRouter extends SelectiveConsumer
{

    public boolean isMatch(MuleEvent event) throws MessagingException
    {
        throw new RuntimeException();
    }

}
