/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.script;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.transformer.TransformerException;
import org.mule.components.script.jsr223.Scriptable;
import org.mule.transformer.AbstractMessageAwareTransformer;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Runs a script to perform transformation on an object.
 */
public class ScriptTransformer extends AbstractMessageAwareTransformer
{
    protected final Scriptable scriptable = new Scriptable();

    public ScriptTransformer()
    {
        super();
    }

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Bindings bindings = this.getScriptEngine().createBindings();
        this.populateBindings(bindings, message);

        try
        {
            return scriptable.runScript(bindings);
        }
        catch (ScriptException e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void populateBindings(Bindings namespace, MuleMessage message)
    {
        namespace.put("message", message);
        namespace.put("src", message.getPayload());
        namespace.put("transformerNamespace", namespace);
        namespace.put("log", logger);
    }

    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     * 
     * @throws org.mule.api.lifecycle.InitialisationException
     */
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return LifecycleTransitionResult.initialiseAll(super.initialise(), new LifecycleTransitionResult.Closure()
        {
            public LifecycleTransitionResult doContinue() throws InitialisationException
            {
                return scriptable.initialise();
            }});
    }

    public ScriptEngine getScriptEngine()
    {
        return scriptable.getScriptEngine();
    }

    public void setScriptEngine(ScriptEngine scriptEngine)
    {
        scriptable.setScriptEngine(scriptEngine);
    }

    public CompiledScript getCompiledScript()
    {
        return scriptable.getCompiledScript();
    }

    public void setCompiledScript(CompiledScript compiledScript)
    {
        scriptable.setCompiledScript(compiledScript);
    }

    public String getScriptText()
    {
        return scriptable.getScriptText();
    }

    public void setScriptText(String scriptText)
    {
        scriptable.setScriptText(scriptText);
    }

    public String getScriptFile()
    {
        return scriptable.getScriptFile();
    }

    public void setScriptFile(String scriptFile)
    {
        scriptable.setScriptFile(scriptFile);
    }

    public void setScriptEngineName(String scriptEngineName)
    {
        scriptable.setScriptEngineName(scriptEngineName);
    }

    public String getScriptEngineName()
    {
        return scriptable.getScriptEngineName();
    }

}
