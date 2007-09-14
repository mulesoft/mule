/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.util.List;

/**
 * <code>ReplyToHandler</code> is used to handle routing where a replyTo endpointUri is
 * set on the message
 */

public interface ReplyToHandler
{

    void processReplyTo(UMOEvent event, UMOMessage returnMessage, Object replyTo) throws UMOException;

    void setTransformers(List transformers);

    List getTransformers();
    
}
