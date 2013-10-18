/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


