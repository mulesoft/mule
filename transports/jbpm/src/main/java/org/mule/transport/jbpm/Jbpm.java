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

    protected ProcessEngine processEngine = null;

    private String configurationResource;
    
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
     * @param The configuration file for jBPM, default is "jbpm.cfg.xml" if not specified.
     */
    public Jbpm(String configurationResource)
    {
        this.configurationResource = configurationResource;
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
    public Object startProcess(Object processType) throws Exception
    {
        return startProcess(processType, /* transition */null, /* processVariables */null);
    }

    /**
     * Start a new process.
     * 
     * @return the newly-created ProcessInstance
     */
    public synchronized Object startProcess(Object processType, Object processInstanceKey, Map processVariables) throws Exception
    {
        ProcessInstance processInstance = processEngine.getExecutionService().startProcessInstanceByKey(
            (String) processType, (Map) processVariables, (String) processInstanceKey);

        if (processInstance == null)
        {
            throw new IllegalArgumentException("No process definition found for process " + processType);
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
        return advanceProcess(executionId, /* transition */null, /* processVariables */null);
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
    public synchronized Object advanceProcess(Object processId, Object transition, Map processVariables) throws Exception
    {
        return processEngine.getExecutionService().signalExecutionById((String) processId, processVariables);
    }

    /**
     * Update the variables for an execution.
     * 
     * @return the updated ProcessInstance
     */
    public synchronized Object updateProcess(Object executionId, Map processVariables) throws Exception
    {
        // Set any process variables.
        if (processVariables != null && !processVariables.isEmpty())
        {
            processEngine.getExecutionService().setVariables((String) executionId, processVariables);
        }

        return processEngine.getExecutionService().findExecutionById((String) executionId).getProcessInstance();
    }

    /**
     * Delete a process instance.
     */
    public synchronized void abortProcess(Object processId) throws Exception
    {
        processEngine.getExecutionService().endProcessInstance((String) processId, Execution.STATE_ENDED);
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
        Set activities = processInstance.findActiveActivityNames();
        if( activities != null && activities.size() > 0 ) {
            return activities.iterator().next();
        }
        return null;
    }

    public boolean hasEnded(Object process) throws Exception
    {
        return ((ProcessInstance) process).isEnded();
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

    /*
     * TODO: in jbpm4 tasks now seem to be gotten via person not whole process instance
    public List <TaskInstance> loadTasks(ProcessInstance processInstance)
    {
        List <TaskInstance> taskInstances;
        taskService.
    }
    */
    
    public void completeTask(Task task)
    {
        completeTask(task, /* transition */null);
    }

    public synchronized void completeTask(Task task, String transition)
    {
        processEngine.getTaskService().completeTask(task.getId());
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

}
