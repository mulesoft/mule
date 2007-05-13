/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm.jbpm;

import org.mule.providers.bpm.BPMS;
import org.mule.providers.bpm.MessageService;
import org.mule.util.IOUtils;
import org.mule.util.NumberUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * jBPM's implementation of Mule's generic BPMS interface.
 * This class should be set as the "bpms" property of the BPM Connector:
 *
 *   <connector name="jBpmConnector" className="org.mule.providers.bpm.ProcessConnector">
 *       <properties>
 *           <spring-property name="bpms">
 *              <ref local="jbpm" />
 *           </spring-property>
 *       </properties>
 *   </connector>
 *
 *   <bean id="jbpm" class="org.mule.providers.bpm.jbpm.Jbpm" destroy-method="destroy">
 *       <spring-property name="jbpmConfiguration">
 *           <ref local="jbpmConfig" />
 *       </spring-property>
 *   </bean>
 */
public class Jbpm implements BPMS
{
    protected static transient Log logger = LogFactory.getLog(Jbpm.class);

    protected JbpmConfiguration jbpmConfiguration = null;

    /**
     * Indicates whether jBPM has been instantiated by the connector (false) or was passed 
     * in from somewhere else (true).
     */
    protected boolean containerManaged;
    
    // ///////////////////////////////////////////////////////////////////////////
    // Lifecycle methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates the Mule wrapper for jBPM using a default configuration. 
     */
    public Jbpm()
    {
        this(JbpmConfiguration.getInstance());
        containerManaged = false;
    }

    /**
     * Creates the Mule BPM wrapper based on an already-initialized jBPM instance
     * 
     * @param jbpmConfiguration - the already-initialized jBPM instance
     */
    public Jbpm(JbpmConfiguration jbpmConfiguration)
    {
        setJbpmConfiguration(jbpmConfiguration);
        containerManaged = true;
    }

    public void destroy()
    {
        if (containerManaged == false && jbpmConfiguration != null)
        {
            jbpmConfiguration.close();
            jbpmConfiguration = null;
        }
    }

    public void setMessageService(MessageService msgService)
    {
        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            ((MuleMessageService)jbpmContext.getServices().getMessageService()).setMessageService(msgService);
        }
        finally
        {
            jbpmContext.close();
        }
    }
    
    // ///////////////////////////////////////////////////////////////////////////
    // Process manipulation
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Start a new process.
     * 
     * @return the newly-created ProcessInstance
     */
    public synchronized Object startProcess(Object processType) throws Exception
    {
        return startProcess(processType, /* transition */null, /* processVariables */null);
    }

    /**
     * Start a new process.
     * 
     * @return the newly-created ProcessInstance
     */
    public synchronized Object startProcess(Object processType, Object transition, Map processVariables) throws Exception
    {
        ProcessInstance processInstance = null;

        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            ProcessDefinition processDefinition = jbpmContext.getGraphSession().findLatestProcessDefinition(
                (String)processType);
            if (processDefinition == null)
                throw new IllegalArgumentException("No process definition found for process " + processType);

            processInstance = new ProcessInstance(processDefinition);

            // Set any process variables.
            if (processVariables != null && !processVariables.isEmpty())
            {
                processInstance.getContextInstance().addVariables(processVariables);
            }
            processInstance.getContextInstance().addVariables(processVariables);

            // Leave the start state.
            processInstance.signal();

            jbpmContext.save(processInstance);

            return processInstance;
        }
        catch (Exception e)
        {
            jbpmContext.setRollbackOnly();
            throw e;
        }
        finally
        {
            jbpmContext.close();
        }
    }

    /**
     * Advance a process instance one step.
     * 
     * @return the updated ProcessInstance
     */
    public synchronized Object advanceProcess(Object processId) throws Exception
    {
        return advanceProcess(processId, /* transition */null, /* processVariables */null);
    }

    /**
     * Advance a process instance one step.
     * 
     * @return the updated ProcessInstance
     */
    public synchronized Object advanceProcess(Object processId, Object transition, Map processVariables)
        throws Exception
    {
        ProcessInstance processInstance = null;

        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            // Look up the process instance from the database.
            processInstance = jbpmContext.getGraphSession()
                .loadProcessInstance(NumberUtils.toLong(processId));

            if (processInstance.hasEnded())
            {
                throw new IllegalStateException(
                    "Process cannot be advanced because it has already terminated, processId = " + processId);
            }

            // Set any process variables.
            // Note: addVariables() will replace the old value of a variable if it
            // already exists.
            if (processVariables != null && !processVariables.isEmpty())
            {
                processInstance.getContextInstance().addVariables(processVariables);
            }

            // Advance the workflow.
            if (transition != null)
            {
                processInstance.signal((String)transition);
            }
            else
            {
                processInstance.signal();
            }

            // Save the process state back to the database.
            jbpmContext.save(processInstance);

            return processInstance;
        }
        catch (Exception e)
        {
            jbpmContext.setRollbackOnly();
            throw e;
        }
        finally
        {
            jbpmContext.close();
        }
    }

    /**
     * Update the variables for a process instance.
     * 
     * @return the updated ProcessInstance
     */
    public synchronized Object updateProcess(Object processId, Map processVariables) throws Exception
    {
        ProcessInstance processInstance = null;

        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            // Look up the process instance from the database.
            processInstance = jbpmContext.getGraphSession()
                .loadProcessInstance(NumberUtils.toLong(processId));

            // Set any process variables.
            // Note: addVariables() will replace the old value of a variable if it
            // already exists.
            if (processVariables != null && !processVariables.isEmpty())
            {
                processInstance.getContextInstance().addVariables(processVariables);
            }

            // Save the process state back to the database.
            jbpmContext.save(processInstance);

            return processInstance;
        }
        catch (Exception e)
        {
            jbpmContext.setRollbackOnly();
            throw e;
        }
        finally
        {
            jbpmContext.close();
        }
    }

    /**
     * Delete a process instance.
     */
    public synchronized void abortProcess(Object processId) throws Exception
    {
        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            jbpmContext.getGraphSession().deleteProcessInstance(NumberUtils.toLong(processId));
        }
        catch (Exception e)
        {
            jbpmContext.setRollbackOnly();
            throw e;
        }
        finally
        {
            jbpmContext.close();
        }
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
        return new Long(((ProcessInstance)process).getId());
    }

    // By default the process is lazily-initialized so we need to open a new session to the
    // database before calling process.getRootToken().getNode().getName()
    public Object getState(Object process) throws Exception
    {
        ProcessInstance processInstance = (ProcessInstance) process;

        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            // Look up the process instance from the database.
            processInstance = jbpmContext.getGraphSession()
                .loadProcessInstance(processInstance.getId());
            return processInstance.getRootToken().getNode().getName();
        }
        finally
        {
            jbpmContext.close();
        }
    }

    public boolean hasEnded(Object process) throws Exception
    {
        return ((ProcessInstance)process).hasEnded();
    }

    /**
     * Look up an already-running process instance.
     * 
     * @return the ProcessInstance
     */
    public Object lookupProcess(Object processId) throws Exception
    {
        ProcessInstance processInstance = null;

        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            // Look up the process instance from the database.
            processInstance = jbpmContext.getGraphSession()
                .loadProcessInstance(NumberUtils.toLong(processId));
            return processInstance;
        }
        finally
        {
            jbpmContext.close();
        }
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Miscellaneous
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Deploy a new process definition.
     */
    public void deployProcess(String processDefinitionFile) throws FileNotFoundException, IOException
    {
        deployProcessFromStream(IOUtils.getResourceAsStream(processDefinitionFile, getClass()));
    }

    public void deployProcessFromStream(InputStream processDefinition)
        throws FileNotFoundException, IOException
    {
        deployProcess(ProcessDefinition.parseXmlInputStream(processDefinition));
    }

    private void deployProcess(ProcessDefinition processDefinition)
    {
        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            jbpmContext.deployProcessDefinition(processDefinition);
        }
        finally
        {
            jbpmContext.close();
        }
    }

    public List/* <TaskInstance> */loadTasks(ProcessInstance process)
    {
        List/* <TaskInstance> */taskInstances = null;

        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            taskInstances = jbpmContext.getTaskMgmtSession().findTaskInstancesByToken(
                process.getRootToken().getId());
            return taskInstances;
        }
        finally
        {
            jbpmContext.close();
        }
    }

    public synchronized void completeTask(TaskInstance task)
    {
        completeTask(task, /* transition */null);
    }

    public synchronized void completeTask(TaskInstance task, String transition)
    {
        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try
        {
            task = jbpmContext.getTaskMgmtSession().loadTaskInstance(task.getId());
            if (transition != null)
            {
                task.end(transition);
            }
            else
            {
                task.end();
            }
        }
        finally
        {
            jbpmContext.close();
        }
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Getters and setters
    // ///////////////////////////////////////////////////////////////////////////

    public JbpmConfiguration getJbpmConfiguration()
    {
        return jbpmConfiguration;
    }

    public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration)
    {
        this.jbpmConfiguration = jbpmConfiguration;
    }
}
