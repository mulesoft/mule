/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.filter;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.module.scripting.component.Scriptable;
import org.mule.processor.AbstractFilteringMessageProcessor;

import javax.script.Bindings;

public class ScriptFilter extends AbstractFilteringMessageProcessor implements Filter
{

    private Scriptable script;
    
    private String name;
    
    @Override
    protected boolean accept(MuleEvent event)
    {
        return this.accept(event.getMessage());
    }
    
    public boolean accept(MuleMessage message)
    {
        Bindings bindings = script.getScriptEngine().createBindings();
        script.populateBindings(bindings, message);
        try
        {
            return (Boolean) script.runScript(bindings);
        }
        catch (Throwable e)
        {
            return false;
        }
    }

    public Scriptable getScript()
    {
        return script;
    }

    public void setScript(Scriptable script)
    {
        this.script = script;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}


