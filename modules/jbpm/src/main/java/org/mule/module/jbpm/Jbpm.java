/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jbpm;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.MessageService;
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
 * An implementation of Mule's generic {@link BPMS} interface for JBoss jBPM.
 */
public class Jbpm implements BPMS, Initialisable, Disposable
{
    /**
     * The initialized jBPM ProcessEngine.
     */
    protected ProcessEngine processEngine = null;

    /**
     * The configuration file for jBPM, default is "jbpm.cfg.xml" if not specified.
     */
    private String configurationResource;

    /**
     * Process definitions to be loaded into jBPM at startup.
     */
    private Properties processDefinitions;

    /**
     * An optional logical name for the BPMS.
     */
    private String name;

    /**
     * Indicates whether jBPM has been instantiated by Mule (false) or was
     * passed in from somewhere else (true).
     */
    protected boolean containerManaged = true;

    public static final String PROCESS_ENDED = "Process has ended";

    /**
     * Given the multi-threaded nature of Mule, sometimes a response message will arrive to advance the
     * process before the creation of the process has fully terminated (e.g., during in-memory unit tests).
     * After this amount of time (in ms), we stop waiting and assume there must be some other problem.
     */
    public static final int PROCESS_CREATION_WAIT = 3000;

    protected static final Logger log = LoggerFactory.getLogger(Jbpm.class);

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
     *
     * @param processEngine The already-initialized jBPM ProcessEngine. This is
     *            useful if you use Spring to configure your jBPM instance.
     */
    public Jbpm(ProcessEngine processEngine, Properties processDefinitions)
    {
        this.processEngine = processEngine;
        this.processDefinitions = processDefinitions;
    }

    @Override
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

    @Override
    public void dispose()
    {
        if (!containerManaged && processEngine != null)
        {
            processEngine.close();
            processEngine = null;
        }
    }

    @Override
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
    @Override
    public Object startProcess(Object processDefinitionKey, Object signalName, Map variables) throws Exception
    {
        ProcessInstance processInstance =
            processEngine.getExecutionService().startProcessInstanceByKey((String) processDefinitionKey, variables);

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
     * @param variables - optional process variables/parameters to set
     * @return the updated ProcessInstance
     */
    @Override
    public Object advanceProcess(Object executionId, Object signalName, Map variables) throws Exception
    {
        int waitTime = 0;
        Execution execution = processEngine.getExecutionService().findExecutionById((String) executionId);
        while (execution == null && waitTime < PROCESS_CREATION_WAIT)
        {
            // Given the multi-threaded nature of Mule, sometimes a response message will arrive to advance the
            // process before the creation of the process has fully terminated (e.g., during in-memory unit tests).
            // We delay for awhile to make sure this is not the case before giving up and throwing an exception.
            Thread.sleep(PROCESS_CREATION_WAIT / 10);
            waitTime += (PROCESS_CREATION_WAIT / 10);
            execution = processEngine.getExecutionService().findExecutionById((String) executionId);
        }
        if (execution == null)
        {
            throw new IllegalArgumentException("No process execution found with id = " + executionId + " (it may have already terminated)");
        }

        String processId;
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

        // MULE-1690
        synchronized (this)
        {
            processEngine.getExecutionService().signalExecutionById((String) executionId, (String) signalName, variables);
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
     * Update the variables for an execution.
     *
     * @return the updated ProcessInstance
     */
    @Override
    public Object updateProcess(Object executionId, Map variables) throws Exception
    {
        // Get Process ID
        String processId;
        Execution execution = processEngine.getExecutionService().findExecutionById((String) executionId);
        if (execution == null)
        {
            throw new IllegalArgumentException("No process execution found with id = " + executionId + " (it may have already terminated)");
        }
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
    @Override
    public void abortProcess(Object processInstanceId) throws Exception
    {
        processEngine.getExecutionService().endProcessInstance((String) processInstanceId, Execution.STATE_ENDED);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Process status / lookup
    // ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isProcess(Object obj) throws Exception
    {
        return (obj instanceof ProcessInstance);
    }

    @Override
    public Object getId(Object process) throws Exception
    {
        return ((ProcessInstance) process).getId();
    }

    @Override
    public Object getState(Object process) throws Exception
    {
        return getState((ProcessInstance) process);
    }

    public static String getState(ProcessInstance processInstance) throws Exception
    {
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

    @Override
    public boolean hasEnded(Object process) throws Exception
    {
        return process == null ? true : ((ProcessInstance) process).isEnded();
    }

    /**
     * Look up an already-running process instance.
     *
     * @return the ProcessInstance
     */
    @Override
    public Object lookupProcess(Object processId) throws Exception
    {
        return processEngine.getExecutionService().findProcessInstanceById((String) processId);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Miscellaneous
    // ///////////////////////////////////////////////////////////////////////////

    @Override
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

    @Override
    public void undeployProcess(String resource) throws Exception
    {
        // empty
    }

    public void completeTask(Task task)
    {
        completeTask(task, null, null);
    }

    public void completeTask(Task task, String outcome, Map variables)
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

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
