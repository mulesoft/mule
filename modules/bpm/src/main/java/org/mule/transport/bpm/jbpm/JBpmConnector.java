/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm.jbpm;

import org.mule.api.MuleContext;
import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.config.BpmNamespaceHandler;
import org.mule.transport.bpm.ProcessConnector;
import org.mule.util.ClassUtils;

import java.util.Properties;

/**
 * @deprecated It is recommended to configure BPM as a component rather than a transport for 3.x
 */
public class JBpmConnector extends ProcessConnector 
{    
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
        return (BPMS) ClassUtils.instanciateClass(BpmNamespaceHandler.JBPM_WRAPPER_CLASS, configurationResource, processDefinitions);
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
