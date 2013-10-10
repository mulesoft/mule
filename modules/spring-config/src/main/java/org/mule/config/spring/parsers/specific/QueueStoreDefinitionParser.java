/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

/**
 * This allows a queue store to be defined globally, or embedded within a queue profile.
 */
public class QueueStoreDefinitionParser extends ParentContextDefinitionParser
{
    public QueueStoreDefinitionParser(Class<?> queueStoreFactoryBeanClass)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, 
            new MuleOrphanDefinitionParser(queueStoreFactoryBeanClass, true));
        otherwise(new ChildDefinitionParser("queue-store", queueStoreFactoryBeanClass));
    }

    /**
     * For custom processors
     */
    public QueueStoreDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(true));
        otherwise(new ChildDefinitionParser("queue-store"));
    }
}
