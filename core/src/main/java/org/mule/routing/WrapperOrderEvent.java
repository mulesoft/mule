/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import java.io.Serializable;

class WrapperOrderEvent implements Serializable
{
    final MuleEvent event ;
    final int arrivalOrder ;

    public WrapperOrderEvent(MuleEvent event, int arrivalOrder)
    {
        this.event = event;
        this.arrivalOrder = arrivalOrder;
    }

}