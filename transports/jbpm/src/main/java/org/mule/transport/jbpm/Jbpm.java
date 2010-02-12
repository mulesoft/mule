/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.transport.bpm.BPMS;
import org.mule.transport.bpm.MessageService;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jbpm.api.Configuration;
import org.jbpm.api.Execution;
import org.jbpm.api.ProcessEngine;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jBPM's implementation of Mule's generic BPMS interface. This class should be set
 * as the "bpms" property of the BPM Connector: <connector name="jBpmConnector"
 * className="org.mule.transport.bpm.ProcessConnector"> <properties> <spring-property
 * name="bpms"> <ref local="jbpm" /> </spring-property> </properties> </connector>
 * <bean id="jbpm" class="org.mule.transport.bpm.jbpm.Jbpm" destroy-method="destroy">
 * <spring-property name="processEngine"> <ref local="processEngine" />
 * </spring-property> </bean>
 */
public class Jbpm implements BPMS, Initialisable, Disposable
{
    protected static final Logger log = LoggerFactory.getLogger(Jbpm.class);

    public static final String PROCESS_ENDED = "Process has ended";
    
    protected ProcessEngine processEngine = null;

    private String configurationResource;
    
    private Properties processDefinitions;
    
    /**
     * Indicates whether jBPM has been instantiated by the connector (false) or was
     * passed in from somewhere else (true).
     */
    protected boolean containerManaged = true;

    // ///////////////////////////////////////////////////////////////////////////
    // Lifecycle methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates the Mule wrapper for jBPM
     */
    public Jbpm()
    {
        // empty
    }

    /**
     * Creates the Mule wrapper for jBPM
     * @param configurationResource - The configuration file for jBPM, default is "jbpm.cfg.xml" if not specified.
     * @param processDefinitions - A list of process definitions to load into jBPM upon initialization.
     */
    public Jbpm(String configurationResource, Properties processDefinitions)
    {
        this.configurationResource = configurationResource;
        this.processDefinitions = processDefinitions;
    }

    /**
     * Creates the Mule wrapper for jBPM
     * @param The already-initialized jBPM ProcessEngine.  This is useful if you use Spring to configure your jBPM instance.
     */
    public Jbpm(ProcessEngine processEngine)
    {
        this.processEngine = processEngine;
    }

    public void initialise()
    {
        if (processEngine == null)
        {
            Configuration config = new Configuration();
            if (configurationResource != null)
            {
                config.setResource(configurationResource);
            }
            setProcessEngine(config.buildProcessEngine());  
            containerManaged = false;
        }
        if (processDefinitions != null)
        {
            for (Object def : processDefinitions.values())
            {
                try
                {
                    deployProcess((String) def);
                }
                catch (IOException e)
                {
                    log.error("Unable to deploy process definition: " + e.getMessage());
                }
            }
        }
    }
    
    public void dispose()
    {
        if (!containerManaged && processEngine != null)
        {
            processEngine.close();
            processEngine = null;
        }
    }

    public void setMessageService(MessageService msgService)
    {
        MuleMessageService serviceProxy = processEngine.get(MuleMessageService.class);
        serviceProxy.setMessageService(msgService);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Process manipulation
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Start a new process.
     * 
     * @return the newly-created ProcessInstance
     */
    public Object startProcess(Object processDefinitionKey) throws Exception
    {
        return startProcess(processDefinitionKey, null, null);
    }
    
    /**
     * Start a new process.
     * 
     * @return the newly-created ProcessInstance
     */
    public synchronized Object startProcess(Object processDefinitionKey, Object signalName, Map variables) throws Exception
    {
        ProcessInstance processInstance = 
            processEngine.getExecutionService().startProcessInstanceByKey((String) processDefinitionKey, (Map) variables);

        if (processInstance == null)
        {
            throw new IllegalArgumentException("No process definition found for process " + processDefinitionKey);
        }

        return processInstance;
    }

    /**
     * Advance a process instance one step.
     * 
     * @return the updated ProcessInstance
     */
    public Object advanceProcess(Object executionId) throws Exception
    {
        return advanceProcess(executionId, null, null);
    }

    /**
     * Advance a process instance one step.
     * 
     * @param processId - if no transition value is provided, this is assumed to be
     *            execution id
     * @param transition - the state of the execution we're looking for
     * @param processVariables - optional process variables/parameters to set
     * @return the updated ProcessInstance
     */
    public synchronized Object advanceProcess(Object executionId, Object signalName, Map parameters) throws Exception
    {
        // Get Process ID
        String processId;
        Execution execution = processEngine.getExecutionService().findExecutionById((String) executionId);
        if (execution.getProcessInstance() != null)
        {
            processId = execution.getProcessInstance().getId();
        }
        else
        {
            processId = execution.getId();
        }

        processEngine.getExecutionService().signalExecutionById((String) executionId, (String) signalName, parameters);

        // Refresh process info. from the DB
        ProcessInstance process = processEngine.getExecutionService().findProcessInstanceById(processId);
        if (process == null)
        {
            // The process has already ended, so we return a mock/skeleton ProcessInstance with the expected ID and state = "ended"
            process = new EndedProcess(processId);
        }
        return process;
    }

    /**
     * Update the variables for an execution.
     * 
     * @return the updated ProcessInstance
     */
    public synchronized Object updateProcess(Object executionId, Map variables) throws Exception
    {
        // Get Process ID
        String processId;
        Execution execution = processEngine.getExecutionService().findExecutionById((String) executionId);
        if (execution.getProcessInstance() != null)
        {
            processId = execution.getProcessInstance().getId();
        }
        else
        {
            processId = execution.getId();
        }

        // Set any process variables.
        if (variables != null && !variables.isEmpty())
        {
            processEngine.getExecutionService().setVariables((String) executionId, variables);
        }

        // Refresh process info. from the DB
        ProcessInstance process = processEngine.getExecutionService().findProcessInstanceById(processId);
        if (process == null)
        {
            // The process has already ended, so we return a mock/skeleton ProcessInstance with the expected ID and state = "ended"
            process = new EndedProcess(processId);
        }
        return process;
    }

    /**
     * Delete a process instance.
     */
    public synchronized void abortProcess(Object processInstanceId) throws Exception
    {
        processEngine.getExecutionService().endProcessInstance((String) processInstanceId, Execution.STATE_ENDED);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Process status / lookup
    // ///////////////////////////////////////////////////////////////////////////

    public boolean isProcess(Object obj) throws Exception
    {
        return (obj instanceof ProcessInstance);
    }

    public Object getId(Object process) throws Exception
    {
        return ((ProcessInstance) process).getId();
    }

    public Object getState(Object process) throws Exception
    {
        ProcessInstance processInstance = (ProcessInstance) process;
        if (processInstance == null || processInstance.isEnded())
        {
            return ProcessInstance.STATE_ENDED;
        }
        
        Set activities = processInstance.findActiveActivityNames();
        String state = null;
        // Separate concurrent paths of execution with a "/"
        for (Object activityName : activities)
        {
            if (state == null)
            {
                state = (String) activityName;
            }
            else
            {
                state += " / " + activityName;
            }
        }
        return state;
    }

    public boolean hasEnded(Object process) throws Exception
    {
        return process == null ? true : ((ProcessInstance) process).isEnded();
    }

    /**
     * Look up an already-running process instance.
     * 
     * @return the ProcessInstance
     */
    public synchronized Object lookupProcess(Object processId) throws Exception
    {
        return processEngine.getExecutionService().findProcessInstanceById((String) processId);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Miscellaneous
    // ///////////////////////////////////////////////////////////////////////////

    public void deployProcess(String processDefinitionFile) throws IOException
    {
        deployProcessFromStream(processDefinitionFile, IOUtils.getResourceAsStream(processDefinitionFile,
            getClass()));
    }

    public void deployProcessFromStream(String resourceName, InputStream processDefinition)
        throws IOException
    {
        processEngine.getRepositoryService().createDeployment()
            .addResourceFromInputStream(resourceName, processDefinition)
            .deploy();
    }

    public void completeTask(Task task)
    {
        completeTask(task, null, null);
    }

    public synchronized void completeTask(Task task, String outcome, Map variables)
    {
        processEngine.getTaskService().completeTask(task.getId(), outcome, variables);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Getters and setters
    // ///////////////////////////////////////////////////////////////////////////

    public ProcessEngine getProcessEngine()
    {
        return processEngine;
    }

    public void setProcessEngine(ProcessEngine processEngine)
    {
        this.processEngine = processEngine;
    }

    public String getConfigurationResource()
    {
        return configurationResource;
    }

    public void setConfigurationResource(String configurationResource)
    {
        this.configurationResource = configurationResource;
    }

    public Properties getProcessDefinitions()
    {
        return processDefinitions;
    }

    public void setProcessDefinitions(Properties processDefinitions)
    {
        this.processDefinitions = processDefinitions;
    }
}
