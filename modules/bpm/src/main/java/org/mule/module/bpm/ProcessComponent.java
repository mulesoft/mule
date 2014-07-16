/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bpm;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.component.AbstractComponent;
import org.mule.config.i18n.MessageFactory;

/**
 * A service backed by the execution of a business process such as jBPM. 
 */
public class ProcessComponent extends AbstractComponent
{
    // Created internally
    protected Process process;
    
    /** The underlying BPMS */
    protected BPMS bpms;

    /** The logical name of the process.  This is used to look up the running process instance from the BPMS. */
    private String name;
    
    /** The resource containing the process definition.  This will be used to deploy the process to the BPMS. */
    private String resource;

    /** This field will be used to correlate messages with processes. */
    private String processIdField;

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (bpms == null)
        {
            try
            {
                bpms = muleContext.getRegistry().lookupObject(BPMS.class);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
        if (bpms == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("The bpms property must be set for this component."), this);
        }
        if (bpms instanceof Initialisable)
        {
            ((Initialisable) bpms).initialise();
        }
        
        process = new Process(bpms, name, resource, flowConstruct, muleContext);
        process.initialise();

        // Inject a callback so that the BPMS may generate messages within Mule.
        bpms.setMessageService(process);        
    }
    
    @Override
    protected void doDispose()
    {
        if (bpms instanceof Disposable)
        {
            ((Disposable) bpms).dispose();
        }

        process.dispose();
        process = null;
    }

    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        return process.handleEvent(event);
    }

    protected Process getProcess()
    {
        return process;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setResource(String resource)
    {
        this.resource = resource;
    }

    public String getResource()
    {
        return resource;
    }
    
    public String getProcessIdField()
    {
        return processIdField;
    }

    public void setProcessIdField(String processIdField)
    {
        this.processIdField = processIdField;
    }

    public BPMS getBpms()
    {
        return bpms;
    }

    public void setBpms(BPMS bpms)
    {
        this.bpms = bpms;
    }
}


