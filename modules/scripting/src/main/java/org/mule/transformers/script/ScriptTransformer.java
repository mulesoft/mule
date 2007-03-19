/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.script;

import org.mule.components.script.jsr223.Scriptable;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Runs a script to perform transformation on an object.
 */
public class ScriptTransformer extends AbstractEventAwareTransformer
{
    protected final Scriptable scriptable = new Scriptable();

    public ScriptTransformer()
    {
        super();
    }

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        Bindings bindings = this.getScriptEngine().createBindings();
        this.populateBindings(bindings, context, src);

        try
        {
            return scriptable.runScript(bindings);
        }
        catch (ScriptException e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void populateBindings(Bindings namespace, UMOEventContext context, Object src)
    {
        namespace.put("context", context);
        namespace.put("message", context.getMessage());
        namespace.put("src", src);
        namespace.put("transformertNamespace", namespace);
        namespace.put("log", logger);
    }

    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     * 
     * @throws org.mule.umo.lifecycle.InitialisationException
     */
    public void initialise() throws InitialisationException
    {
        super.initialise();
        scriptable.initialise();
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
