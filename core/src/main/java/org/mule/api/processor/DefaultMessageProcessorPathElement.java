/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        this.children = new ArrayList<MessageProcessorPathElement>();
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
        return Collections.unmodifiableList(children);
    }

    @Override
    public MessageProcessorPathElement addChild(MessageProcessor mp)
    {
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
