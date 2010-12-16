/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.activiti.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.module.activiti.ActivitiConnector;
import org.mule.module.activiti.action.CreateProcessAction;
import org.mule.module.activiti.action.ListAssignedTasksAction;
import org.mule.module.activiti.action.ListCandidateGroupTasksAction;
import org.mule.module.activiti.action.ListCandidateTasksAction;
import org.mule.module.activiti.action.ListProcessDefinitionsAction;
import org.mule.module.activiti.action.PerformTaskOperationAction;
import org.mule.module.activiti.transformer.JsonToProcessDefinitions;
import org.mule.module.activiti.transformer.JsonToTasks;

/**
 * Registers a Bean Definition Parser for handling <code><activiti:connector></code> elements
 * and supporting endpoint elements.
 */
public class ActivitiNamespaceHandler extends AbstractMuleNamespaceHandler
{
    private static final String DESCRIPTION = "description";

    public void init()
    {
        registerStandardTransportEndpoints(ActivitiConnector.ACTIVITI, new String[]{DESCRIPTION}).addAlias(DESCRIPTION, URIBuilder.PATH);
        registerConnectorDefinitionParser(ActivitiConnector.class);
        
        //INBOUND ACTIONS
        registerBeanDefinitionParser("list-process-definitions", new ActionChildDefinitionParser("action", ListProcessDefinitionsAction.class));
        registerBeanDefinitionParser("list-assigned-tasks", new ActionChildDefinitionParser("action", ListAssignedTasksAction.class));
        registerBeanDefinitionParser("list-candidate-tasks", new ActionChildDefinitionParser("action", ListCandidateTasksAction.class));
        registerBeanDefinitionParser("list-candidate-group-tasks", new ActionChildDefinitionParser("action", ListCandidateGroupTasksAction.class));
        
        //OUTBOUND ACTIONS
        registerBeanDefinitionParser("create-process", new ActionChildDefinitionParser("action", CreateProcessAction.class));
        registerBeanDefinitionParser("perform-task-operation", new ActionChildDefinitionParser("action", PerformTaskOperationAction.class));
    
        //TRANSFORMERS
        registerBeanDefinitionParser("json-to-process-definitions", new MessageProcessorDefinitionParser(JsonToProcessDefinitions.class));
        registerBeanDefinitionParser("json-to-tasks", new MessageProcessorDefinitionParser(JsonToTasks.class));
    }
}
