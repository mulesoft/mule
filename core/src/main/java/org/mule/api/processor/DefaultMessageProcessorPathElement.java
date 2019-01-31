/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class DefaultMessageProcessorPathElement implements MessageProcessorPathElement
{

    private MessageProcessorPathElement parent;
    private List<MessageProcessorPathElement> children;
    private MessageProcessor messageProcessor;
    private String name;

    public DefaultMessageProcessorPathElement(MessageProcessor messageProcessor, String name)
    {
        this.messageProcessor = messageProcessor;
        this.name = escape(name);
        this.children = synchronizedList(new ArrayList<MessageProcessorPathElement>());
    }

    @Override
    public MessageProcessorPathElement getParent()
    {
        return parent;
    }

    @Override
    public void setParent(MessageProcessorPathElement parent)
    {

        this.parent = parent;
    }

    @Override
    public List<MessageProcessorPathElement> getChildren()
    {
        return ImmutableList.copyOf(children);
    }

    @Override
    public MessageProcessorPathElement addChild(MessageProcessor mp)
    {
        if (alreadyAddedChild(mp))
        {
            return null;
        }
        int size = children.size();
        DefaultMessageProcessorPathElement result = new DefaultMessageProcessorPathElement(mp, String.valueOf(size));
        addChild(result);
        return result;
    }

    @Override
    public MessageProcessorPathElement addChild(String name)
    {
        DefaultMessageProcessorPathElement result = new DefaultMessageProcessorPathElement(null, name);
        addChild(result);
        return result;
    }

    public MessageProcessor getMessageProcessor()
    {
        return messageProcessor;
    }

    /**
     * MULE-11358: since the ForEach does not wrapps its components in any wrapper if there is a filter inside of it,
     * wrapped with a MessageFilter, it would end duplicating the path elements for the filter and the message
     * processors that come after it.
     */
    private boolean alreadyAddedChild(MessageProcessor messageProcessor)
    {
        if (messageProcessor == null)
        {
            return false;
        }
        for (MessageProcessorPathElement child : children)
        {
            if (messageProcessor.equals(child.getMessageProcessor()))
            {
                return true;
            }
        }
        return false;
    }

    public void addChild(MessageProcessorPathElement mp)
    {
        children.add(mp);
        mp.setParent(this);
    }

    @Override
    public String getName()
    {
        return name;
    }

    public String getPath()
    {
        return parent == null ? "/" + getName() : parent.getPath() + "/" + getName();
    }

    private String escape(String name)
    {
        StringBuilder builder = new StringBuilder(name.length() * 2);
        char previous = ' ';
        for (char c : name.toCharArray())
        {
            builder.append(c == '/' && previous != '\\' ? "\\/" : c);
            previous = c;
        }
        return builder.toString();
    }
}
