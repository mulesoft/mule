/*
 * $Id: HttpsHandshakeTimingTestCase.java 25119 2012-12-10 21:20:57Z pablo.lagreca $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * Extension of {@link FlowProcessingPhaseTemplate} for those {@link org.mule.api.source.MessageSource}
 * that requires sending a response of the message processed.
 */
public interface RequestResponseFlowProcessingPhaseTemplate extends FlowProcessingPhaseTemplate
{

    /**
     * Template method to send a response after processing the message.
     *
     * This method is executed outside the flow. In case of failure the {@link org.mule.api.exception.SystemExceptionHandler}
     * will be executed.
     *
     * @param muleEvent the event with the content of the response to be sent.
     * @throws MuleException exception thrown during the response is being sent.
     */
    public void sendResponseToClient(MuleEvent muleEvent) throws MuleException;

}
