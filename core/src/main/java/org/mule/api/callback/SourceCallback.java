/**
 * Mule Development Kit
 * Copyright 2010-2012 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * This software is protected under international copyright law. All use of this software is
 * subject to MuleSoft's Master Subscription Agreement (or other master license agreement)
 * separately entered into in writing between you and MuleSoft. If such an agreement is not
 * in place, you may not use the software.
 */



package org.mule.api.callback;

import java.util.Map;

/**
 * Callback interface used by message source annotated methods to dispatch messages.
 */
public interface SourceCallback {
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
