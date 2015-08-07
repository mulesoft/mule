/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.transformer.TransformerException;
import org.mule.module.scripting.component.Scriptable;
import org.mule.transformer.AbstractMessageTransformer;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a script to perform transformation on an object.
 */
public class ScriptTransformer extends AbstractMessageTransformer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptTransformer.class);

    private Scriptable script;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        LifecycleUtils.initialiseIfNeeded(script, muleContext);
    }

    @Override
    public void dispose()
    {
        super.dispose();
        LifecycleUtils.disposeIfNeeded(script, LOGGER);
    }

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
