/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm.jbpm;

import org.mule.api.MuleContext;
import org.mule.transport.bpm.BPMS;
import org.mule.transport.bpm.ProcessConnector;
import org.mule.util.ClassUtils;

import java.util.Properties;

public class JBpmConnector extends ProcessConnector 
{
    public static final String JBPM_WRAPPER_CLASS = "org.mule.transport.jbpm.Jbpm";
    
    private String configurationResource;
    
    private Object processEngine;
    
    private Properties processDefinitions;

    public JBpmConnector(MuleContext context)
    {
        super(context);
    }
    
    @Override
    protected BPMS createBpms() throws Exception
    {
        return (BPMS) ClassUtils.instanciateClass(JBPM_WRAPPER_CLASS, configurationResource, processDefinitions);
    }

    public void setConfigurationResource(String configurationResource)
    {
        this.configurationResource = configurationResource;
    }

    public String getConfigurationResource()
    {
        return configurationResource;
    }
    
    public Object getProcessEngine()
    {
        return processEngine;
    }

    public void setProcessEngine(Object processEngine)
    {
        this.processEngine = processEngine;
    }
    
    public void setProcessDefinitions(Properties processDefinitions)
    {
        this.processDefinitions = processDefinitions;
    }
}
