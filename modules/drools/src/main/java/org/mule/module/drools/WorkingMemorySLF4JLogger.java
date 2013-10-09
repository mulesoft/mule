/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
