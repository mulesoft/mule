/**
 *
 * (c) 2013 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        this.name = name;
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
        return parent == null ? "/"+getName() : parent.getPath() + "/" + getName();
    }
}
