/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import java.util.Map;

/**
 * Callback interface used by message processors methods. <p/>
 * The method parameters of type {@link NestedProcessor} will be able to receive other
 * message processors.
 */
public interface NestedProcessor
{

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
     * @param payload              The payload of the message
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
