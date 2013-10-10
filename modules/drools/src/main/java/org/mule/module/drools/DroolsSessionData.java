/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.drools;

import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.runtime.StatefulKnowledgeSession;

/**
 * A simple data structure which contains the KnowledgeSession plus any other stateful information.
 */
public class DroolsSessionData
{
    private final StatefulKnowledgeSession session;
    
    private final KnowledgeRuntimeLogger logger;

    public DroolsSessionData(StatefulKnowledgeSession session, KnowledgeRuntimeLogger logger)
    {
        this.session = session;
        this.logger = logger;
    }
    
    public StatefulKnowledgeSession getSession()
    {
        return session;
    }

    public KnowledgeRuntimeLogger getLogger()
    {
        return logger;
    }
}


