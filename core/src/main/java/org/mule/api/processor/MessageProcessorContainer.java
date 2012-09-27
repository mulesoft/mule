/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import java.util.Map;

/**
 *  Identifies Constructs that contain Message Processors configured by the user.
 */
public interface MessageProcessorContainer
{
    /**
     * Generates a map of the child message processors with the message processor
     * instance as key and the identifier path as value.
     *
     * @return Map with the paths of the child message processors
     */
    Map<MessageProcessor, String> getMessageProcessorPaths();
}
