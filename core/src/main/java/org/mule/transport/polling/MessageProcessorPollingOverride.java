/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

/**
 * Extension point to hook extra functionality to a message processor polling component.
 *
 * Implementation registered with Poll will be called on each execution to provide an interceptor which
 * will then be called on several execution checkpoints.
 * Note that while this class should be threadsafe, interceptors returned will be scoped on a single execution, and
 * hence may carry state.
 */
public abstract class MessageProcessorPollingOverride
{

    /**
     * Returns an interceptor instance.
     * This method will be called on every poll, and may return a new instance every time or always the same instance
     * (the latter case requires the instance to be threadsafe). Interceptor's are scoped for each poll and flow execution
     * and will be discarded after the scope ends.
     * @return a new interceptor instance that cn be used to alter the functionality of a message processor polling component
     */
    public abstract MessageProcessorPollingInterceptor interceptor();


    /**
     * Override implementation that doesn't change anything. Used as a default when no override is defined
     */
    public static class NullOverride extends MessageProcessorPollingOverride
    {

        private MessageProcessorPollingInterceptor noOpInterceptor = new MessageProcessorPollingInterceptor()
        {
        };

        @Override
        public MessageProcessorPollingInterceptor interceptor()
        {
            return noOpInterceptor;
        }
    }

}
