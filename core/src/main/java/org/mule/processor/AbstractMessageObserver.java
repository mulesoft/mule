/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Can be used for simple {@link MessageProcessor}s that require the
 * {@link MuleEvent} to log or send notifications etc. but don't modify it in any way
 * or pass it on to another message processor.
 */
public abstract class AbstractMessageObserver implements MessageProcessor
{

    protected Log logger = LogFactory.getLog(getClass());

    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        // TODO Make event/message immutable before invoking observe
        observe(event);
        return event;
    }

    public abstract void observe(MuleEvent event);
    
    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
