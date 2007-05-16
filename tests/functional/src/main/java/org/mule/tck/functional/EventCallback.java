/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.umo.UMOEventContext;

/**
 * The scope of the Event Callback is to be able to get a the message currently
 * being processed by the {@link FunctionalTestComponent} and make assertions
 * on the message payload, headers or attachments or to make changes required
 * for the test.
 *
 * @see org.mule.tck.functional.FunctionalTestComponent
 */

public interface EventCallback
{
    public void eventReceived(UMOEventContext context, Object component) throws Exception;
}
