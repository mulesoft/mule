/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

public class TestComponent implements Callable
{

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
       if (eventContext.getMessageAsString().equals("quartz test")) {
            if (QuartzReceiverFunctionalTestCase.countDown != null) {
                QuartzReceiverFunctionalTestCase.countDown.unlock();
            }
        } else {
            throw new IllegalArgumentException("Unrecognised event payload");
        }
        return null;
    }

}
