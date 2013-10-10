/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    void addMessages(List messages);

    void removedMessage(MuleMessage message);

    MuleMessage[] getMessagesAsArray();

    MuleMessage getMessage(int index);

    Object[] getPayloadsAsArray();

    int size();
}
