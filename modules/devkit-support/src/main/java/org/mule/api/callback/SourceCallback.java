/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.callback;

import java.util.Map;

/**
 * Callback interface used by message source annotated methods to dispatch messages.
 */
public interface SourceCallback
{

    /**
     * Dispatch the current event to the flow
     *
     * @return The response of the flow
     */
    Object process() throws Exception;

    /**
     * Dispatch message to the flow
     *
     * @param payload The payload of the message
     * @return The response of the flow
     */
    Object process(Object payload) throws Exception;

    /**
     * Dispatch message to the flow with properties
     *
     * @param payload    The payload of the message
     * @param properties Properties to be attached with inbound scope
     * @return The response of the flow
     */
    Object process(Object payload, Map<String, Object> properties) throws Exception;

    /**
     * Dispatch the current event to the flow
     *
     * @return The response of the flow
     */
    org.mule.api.MuleEvent processEvent(org.mule.api.MuleEvent event) throws org.mule.api.MuleException;

}
