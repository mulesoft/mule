/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bpm.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.module.bpm.ProcessComponent;

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
            logger.warn(e.getMessage());
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
}

