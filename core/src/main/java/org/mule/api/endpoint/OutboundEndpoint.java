/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.endpoint;

import org.mule.api.processor.MessageProcessor;

import java.util.List;

public interface OutboundEndpoint extends ImmutableEndpoint, MessageProcessor
{    
    /**
     * @return a list of properties which should be carried over from the request message to the response message
     * in the case of a synchronous call.
     */
    List<String> getResponseProperties();
}


