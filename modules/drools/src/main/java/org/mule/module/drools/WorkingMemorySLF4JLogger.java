/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.drools;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.drools.WorkingMemoryEventManager;
import org.drools.audit.WorkingMemoryLogger;
import org.drools.audit.event.LogEvent;
import org.drools.event.KnowledgeRuntimeEventManager;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.slf4j.Logger;

/**
 * Simple implementation used to send an audit trail of the working memory to the SLF4J logger.
 */
public class WorkingMemorySLF4JLogger extends WorkingMemoryLogger implements KnowledgeRuntimeLogger 
{
    protected final Logger logger;
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
    }

    public WorkingMemorySLF4JLogger(WorkingMemoryEventManager workingMemory, Logger logger)
    {
        super(workingMemory);
        this.logger = logger;
    }

    public WorkingMemorySLF4JLogger(KnowledgeRuntimeEventManager session, Logger logger)
    {
        super(session);
        this.logger = logger;
    }

    public void logEventCreated(LogEvent logEvent)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(logEvent.toString());
        }
    }

    public void close()
    {
        // empty
    }
}
