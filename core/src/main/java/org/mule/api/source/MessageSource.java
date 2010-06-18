/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.source;

import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

/**
 * Implemented by objects that receives or generates messages which are then
 * processed by a {@link MessageProcessor}.
 * 
 * @since 3.0
 */
public interface MessageSource
{
    /**
     * Set the MessageProcessor listener on a message source which will be invoked
     * when a message is received or generated.
     * 
     * @param event MuleEvent to be processed
     * @return optional response MuleEvent
     * @throws MuleException
     */
    void setListener(MessageProcessor listener);
}
