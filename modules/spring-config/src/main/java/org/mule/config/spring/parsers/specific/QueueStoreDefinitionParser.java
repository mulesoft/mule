/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    public QueueStoreDefinitionParser(Class queueStore)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(queueStore, true));
        otherwise(new ChildDefinitionParser("queue-store", queueStore));
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