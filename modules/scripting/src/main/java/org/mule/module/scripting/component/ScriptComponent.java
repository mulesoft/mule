/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.component;

import org.mule.DefaultMuleMessage;
import org.mule.transport.NullPayload;
import org.mule.transformer.TransformerTemplate;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.component.AbstractComponent;

import java.util.Collections;

import javax.script.Bindings;
import javax.script.ScriptEngine;

/**
 * A Script service backed by a JSR-223 compliant script engine such as
 * Groovy, JavaScript, or Rhino.
 */
public class ScriptComponent extends AbstractComponent 
{
    private Scriptable script;

    //@Override
    protected MuleMessage doOnCall(MuleEvent event) throws Exception
    {
        // Set up initial script variables.
        Bindings bindings = script.getScriptEngine().createBindings();
        script.populateBindings(bindings, event);
        Object result = script.runScript(bindings);

        if (result != null)
        {
            if (result instanceof MuleMessage)
            {
                return (MuleMessage) result;
            }
            else
            {
                event.getMessage().applyTransformers(Collections.singletonList(new TransformerTemplate(
                        new TransformerTemplate.OverwitePayloadCallback(result))));
                return event.getMessage();
            }
        }
        else
        {
            return new DefaultMuleMessage(NullPayload.getInstance());
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
