/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


