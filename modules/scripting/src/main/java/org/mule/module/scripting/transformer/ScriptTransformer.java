/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.scripting.component.Scriptable;
import org.mule.transformer.AbstractMessageTransformer;

import javax.script.ScriptException;
import javax.script.Bindings;

/**
 * Runs a script to perform transformation on an object.
 */
public class ScriptTransformer extends AbstractMessageTransformer
{
    private Scriptable script;

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Bindings bindings = script.getScriptEngine().createBindings();
        script.populateBindings(bindings, message);
        try
        {
            return script.runScript(bindings);
        }
        catch (ScriptException e)
        {
            throw new TransformerException(this, e);
        } finally {
            bindings.clear();
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
}
