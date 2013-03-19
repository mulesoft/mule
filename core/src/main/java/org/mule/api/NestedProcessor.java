/**
 * Mule Development Kit
 * Copyright 2010-2012 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * This software is protected under international copyright law. All use of this software is
 * subject to MuleSoft's Master Subscription Agreement (or other master license agreement)
 * separately entered into in writing between you and MuleSoft. If such an agreement is not
 * in place, you may not use the software.
 */



package org.mule.api;

import java.util.Map;

/**
 * Callback interface used by message processors methods. <p/>
The method parameters of type {@link NestedProcessor} will be able to receive other
message processors.
 */
public interface NestedProcessor {
    /**
     * Dispatch original message to the processor chain
     *
     * @param invocationProperties Additional invocation properties
     * @return The return payload for the processor chain
     */
    Object processWithExtraProperties(Map<String, Object> invocationProperties) throws Exception;

    /**
     * Dispatch message to the processor chain
     *
     * @param payload    The payload of the message
     * @param invocationProperties Additional invocation properties
     * @return The return payload for the processor chain
     */
    Object process(Object payload, Map<String, Object> invocationProperties) throws Exception;

    /**
     * Dispatch message to the processor chain
     *
     * @param payload The payload of the message
     * @return The return payload for the processor chain
     */
    Object process(Object payload) throws Exception;

    /**
     * Dispatch original message to the processor chain
     *
     * @return The return payload for the processor chain
     */
    Object process() throws Exception;
}
