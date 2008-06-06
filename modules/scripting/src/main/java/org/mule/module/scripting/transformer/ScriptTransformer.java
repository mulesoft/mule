/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.scripting.component.Scriptable;
import org.mule.transformer.AbstractMessageAwareTransformer;

import javax.script.ScriptException;
import javax.script.Bindings;

/**
 * Runs a script to perform transformation on an object.
 */
public class ScriptTransformer extends AbstractMessageAwareTransformer
{
    private Scriptable script;

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
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
