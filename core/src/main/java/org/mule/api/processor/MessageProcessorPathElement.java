/**
 *
 * (c) 2013 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.processor;

import java.util.List;

public interface MessageProcessorPathElement
{


    public MessageProcessorPathElement getParent();

    public void setParent(MessageProcessorPathElement parent);

    public List<MessageProcessorPathElement> getChildren();

    public MessageProcessorPathElement addChild(MessageProcessor mp);

    public MessageProcessorPathElement addChild(String name);

    public MessageProcessor getMessageProcessor();

    public String getName();

    public String getPath();


}
