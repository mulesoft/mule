/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import java.util.List;

/**
 * An interface that defines a collection of Mule Messages and methods for working with the collection.
 * Typically this type of message is only used when users explicitly want to work with aggregated or re-sequenced
 * collections of messages.
 */
public interface MuleMessageCollection extends MuleMessage
{
    void addMessage(MuleMessage message);

    void addMessage(MuleMessage message, int index);

    void addMessages(MuleMessage[] messages);

    void addMessages(MuleEvent[] events);

    void addMessages(List<MuleMessage> messages);

    void removedMessage(MuleMessage message);

    MuleMessage[] getMessagesAsArray();

    MuleMessage getMessage(int index);

    Object[] getPayloadsAsArray();

    int size();

    /**
     * Returns the list of  {@link MuleMessage} instances that make up this message collection.  If the payload is
     * invalid then the value returned by this method may be inconsistent with {@link #getPayload()}.
     *
     * @return list of {@link MuleMessage} instances.
     */
    List<MuleMessage> getMessageList();

    /**
     * Determines if the message collection payload has been invalidated.  This occurs when the original payload, a
     * collection of {@link MuleMessage}'s is replaced using {@link MuleMessageCollection#setPayload(Object)}.
     *
     * @return true is the payload of this collection has been invalidated.
     */
    boolean isInvalidatedPayload();
}
