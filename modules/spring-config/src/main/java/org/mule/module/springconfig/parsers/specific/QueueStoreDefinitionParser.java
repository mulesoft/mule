/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.parsers.delegate.ParentContextDefinitionParser;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.parsers.generic.MuleOrphanDefinitionParser;

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
