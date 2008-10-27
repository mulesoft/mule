/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.expression;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageAdapter;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.scripting.component.Scriptable;
import org.mule.util.expression.ExpressionEvaluator;

import java.util.Map;
import java.util.WeakHashMap;

import javax.script.Bindings;
import javax.script.ScriptException;

/**
 * An abstract {@link org.mule.util.expression.ExpressionEvaluator} that can be used for any JSR-233 script engine.
 *
 * If a POJO is passed in it is accessible from the 'payload' namespace.  If a {@link MuleMessage} instance is used then
 * it is accessible from the message' namespace and the 'payload' namespace is also available.
 */
public abstract class AbstractScriptExpressionEvaluator implements ExpressionEvaluator, Disposable
{
    protected Map cache = new WeakHashMap(8);

    /**
     * Extracts a single property from the message
     *
     * @param expression the property expression or expression
     * @param message    the message to extract from
     * @return the result of the extraction or null if the property was not found
     */
    public Object evaluate(String expression, MessageAdapter message)
    {
        Scriptable script = getScript(expression);
        Bindings bindings = script.getScriptEngine().createBindings();
        if (message instanceof MuleMessage)
        {
            script.populateBindings(bindings, (MuleMessage) message);
        }
        else 
        {
            script.populateBindings(bindings, message);
        }

        try
        {
            return script.runScript(bindings);
        }
        catch (ScriptException e)
        {
            return null;
        }
    }

    /**
     * Sets the name of the object
     *
     * @param name the name of the object
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }

    protected Scriptable getScript(String expression)
    {
        Scriptable script = (Scriptable)cache.get(expression);
        if(script==null)
        {
            script = new Scriptable();
            script.setScriptEngineName(getName());
            script.setScriptText(expression);
            try
            {
                script.initialise();
            }
            catch (InitialisationException e)
            {
                throw new MuleRuntimeException(
                    CoreMessages.initialisationFailure("An error occurred initialising script."), e);
            }
            cache.put(expression, script);
        }
        return script;
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
    public void dispose()
    {
        cache.clear();
    }
}