/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.drools;

import org.mule.api.config.ConfigurationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.bpm.MessageService;
import org.mule.module.bpm.Rules;
import org.mule.module.bpm.RulesEngine;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.conf.KnowledgeBaseOption;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Drools implements RulesEngine
{
    /** An optional logical name for the Rules Engine. */
    private String name;

    /** A callback to generate Mule messages from Drools */
    private MessageService messageService;

    protected static final Logger logger = LoggerFactory.getLogger(Drools.class);

    /**
     * @return DroolsSessionData - contains the KnowledgeSession plus any other stateful information
     */
    public Object createSession(Rules rules) throws Exception
    {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(null, Thread.currentThread().getContextClassLoader()));
        
        String rulesFile = rules.getResource();
        InputStream is = IOUtils.getResourceAsStream(rulesFile, getClass());
        if (is == null)
        {
            throw new IOException(CoreMessages.cannotLoadFromClasspath(rulesFile).getMessage());
        }
        kbuilder.add(ResourceFactory.newInputStreamResource(is), ResourceType.DRL);
        if (kbuilder.hasErrors())
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("Errors while parsing ruleset defined in file " + rulesFile + " : " + kbuilder.getErrors().toString()));
        }        

        KnowledgeBaseConfiguration conf = 
            KnowledgeBaseFactory.newKnowledgeBaseConfiguration(null, Thread.currentThread().getContextClassLoader());
        if (rules.getConfiguration() != null)
        {
            conf.setOption((KnowledgeBaseOption) rules.getConfiguration());
        }
        else if (rules.isCepMode())
        {
            conf.setOption(EventProcessingOption.STREAM);
        }
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase(conf);
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());            
        
        if (rules.isStateless())
        {
            // TODO Add support for stateless sessions, for now we assume all sessions are stateful.
            throw new ConfigurationException(MessageFactory.createStaticMessage("Stateless sessions are not yet supported"));
        }
        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession();
        KnowledgeRuntimeLogger droolsLogger = new WorkingMemorySLF4JLogger(session, logger);
        
        if (messageService == null)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("MessageService is not available"));
        }
        session.setGlobal("mule", messageService);
        
        session.fireAllRules();
        
        return new DroolsSessionData(session, droolsLogger);
    }
    
    public void disposeSession(Object sessionData) throws Exception
    {
        ((DroolsSessionData) sessionData).getSession().dispose();
        ((DroolsSessionData) sessionData).getLogger().close();
    }
    
    public Object assertFact(Rules rules, Object fact) throws Exception
    {
        StatefulKnowledgeSession session = ((DroolsSessionData) rules.getSessionData()).getSession();
        FactHandle handle = session.getFactHandle(fact);
        if (handle != null)
        {
            session.update(handle, fact);
            session.fireAllRules();
        }
        else
        {
            handle = session.insert(fact);
            session.fireAllRules();
        }
        return handle;
    }
    
    public void retractFact(Rules rules, Object fact) throws Exception
    {
        StatefulKnowledgeSession session = ((DroolsSessionData) rules.getSessionData()).getSession();
        FactHandle handle = session.getFactHandle(fact);
        if (handle != null)
        {
            session.retract(handle);
            session.fireAllRules();
        }
        else
        {
            logger.warn("Unable to retract fact " + fact + " because it is not in the knowledge base");
        }
    }
    
    public Object assertEvent(Rules rules, Object event, String entryPoint) throws Exception
    {
        StatefulKnowledgeSession session = ((DroolsSessionData) rules.getSessionData()).getSession();
        WorkingMemoryEntryPoint wmEntryPoint;
        if (entryPoint != null)
        {
            wmEntryPoint = session.getWorkingMemoryEntryPoint(entryPoint);
        }
        else
        {
            Collection entryPoints = session.getWorkingMemoryEntryPoints();
            if (entryPoints.size() > 1)
            {
                throw new ConfigurationException(MessageFactory.createStaticMessage("Rules contain more than one entry point but none has been specified"));
            }
            wmEntryPoint = (WorkingMemoryEntryPoint) entryPoints.iterator().next();
        }
        
        FactHandle handle = session.getFactHandle(event);
        if (handle != null)
        {
            wmEntryPoint.update(handle, event);
        }
        else
        {
            handle = wmEntryPoint.insert(event);
        }
        session.fireAllRules();
        return handle;
    }
    
    public void setMessageService(MessageService messageService)
    {
        this.messageService = messageService;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}


