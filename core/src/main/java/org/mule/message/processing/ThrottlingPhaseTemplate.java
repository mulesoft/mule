/*
 * $Id: HttpsHandshakeTimingTestCase.java 25119 2012-12-10 21:20:57Z pablo.lagreca $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message.processing;

import org.mule.api.MuleException;

/**
 * Template that a {@link org.mule.api.source.MessageSource} must implement
 * if it wants to participate in the throttling phase when processing a message
 */
public interface ThrottlingPhaseTemplate extends FlowProcessingPhaseTemplate
{

    /**
     * Discards the message due to ThrottlingPolicy configured for the {@link org.mule.api.source.MessageSource} is exceeded
     *
     * @throws MuleException
     */
    void discardMessageOnThrottlingExceeded() throws MuleException;

}
