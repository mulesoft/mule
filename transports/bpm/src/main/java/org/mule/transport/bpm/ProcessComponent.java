/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.component.AbstractComponent;

import org.apache.commons.lang.NotImplementedException;

// TODO MULE-3205
public class ProcessComponent extends AbstractComponent
{
    //@Override
    protected MuleMessage doOnCall(MuleEvent event) throws Exception
    {
        throw new NotImplementedException();
    }

}


