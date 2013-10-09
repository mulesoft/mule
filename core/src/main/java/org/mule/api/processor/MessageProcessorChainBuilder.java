/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.processor;


/**
 * Builds {@link org.mule.api.processor.MessageProcessorChain} instances.
 *
 * @since 3.1
 */
public interface MessageProcessorChainBuilder extends MessageProcessorBuilder
{
    MessageProcessorChainBuilder chain(MessageProcessor... processors);

    MessageProcessorChainBuilder chain(MessageProcessorBuilder... builders);
}
