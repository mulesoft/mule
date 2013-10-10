/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.bpm.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.module.bpm.ProcessComponent;
import org.mule.module.bpm.RulesComponent;

/**
 * Registers a Bean Definition Parsers for the "bpm" namespace.
 */
public class BpmNamespaceHandler extends AbstractMuleNamespaceHandler
{
    /** 
     * Allows simple configuration of jBPM from the generic "bpm" namespace.  Otherwise you would need to include both the 
     * "bpm" and "jbpm" namespaces in your config, which is not really justified.
     */
    public static final String JBPM_WRAPPER_CLASS = "org.mule.module.jbpm.Jbpm";

    /** 
     * Allows simple configuration of Drools from the generic "bpm" namespace.  Otherwise you would need to include both the 
     * "bpm" and "drools" namespaces in your config, which is not really justified.
     */
    public static final String DROOLS_WRAPPER_CLASS = "org.mule.module.drools.Drools";

    public void init()
    {
        registerBeanDefinitionParser("process", new ProcessComponentDefinitionParser());
        registerMuleBeanDefinitionParser("process-definition", new ChildMapEntryDefinitionParser("processDefinitions", "name", "resource"));
        try
        {
            registerBeanDefinitionParser("jbpm", new MuleOrphanDefinitionParser(Class.forName(JBPM_WRAPPER_CLASS), true));
        }
        catch (ClassNotFoundException e)
        {
            logger.debug("Element <bpm:jbpm> will not available because " + JBPM_WRAPPER_CLASS + " is not on the classpath");
        }

        registerBeanDefinitionParser("rules", new RulesComponentDefinitionParser());
        try
        {
            registerBeanDefinitionParser("drools", new MuleOrphanDefinitionParser(Class.forName(DROOLS_WRAPPER_CLASS), true));
        }
        catch (ClassNotFoundException e)
        {
            logger.debug("Element <drools> will not available in the bpm: namespace because it is not on the classpath");
        }
    }
    
    class ProcessComponentDefinitionParser extends ComponentDefinitionParser
    {
        public ProcessComponentDefinitionParser()
        {
            super(ProcessComponent.class);
            addAlias("processName", "name");
            addAlias("processDefinition", "resource");
        }
    }

    class RulesComponentDefinitionParser extends ComponentDefinitionParser
    {
        public RulesComponentDefinitionParser()
        {
            super(RulesComponent.class);
            addAlias("rulesDefinition", "resource");
        }
    }
}

