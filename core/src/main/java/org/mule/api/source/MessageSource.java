/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.source;

import org.mule.api.processor.MessageProcessor;

/**
 * Implemented by objects that receives or generates messages which are then
 * processed by a {@link MessageProcessor}.
 *
 * @since 3.0
 */
public interface MessageSource
{
    /**
     * Set the MessageProcessor listener on a message source which will be invoked
     * when a message is received or generated.
     */
    void setListener(MessageProcessor listener);
}
