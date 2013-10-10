/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.scripting.component.Scriptable;

import java.util.Map;
import java.util.WeakHashMap;

import javax.script.Bindings;
import javax.script.ScriptException;

/**
 * An abstract {@link org.mule.api.expression.ExpressionEvaluator} that can be used for any JSR-233 script engine.
 *
 * If a POJO is passed in it is accessible from the 'payload' namespace.  If a {@link MuleMessage} instance is used then
 * it is accessible from the message' namespace and the 'payload' namespace is also available.
 */
public abstract class AbstractScriptExpressionEvaluator implements ExpressionEvaluator, Disposable, MuleContextAware
{
    protected Map cache = new WeakHashMap(8);

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Extracts a single property from the message
     *
     * @param expression the property expression or expression
     * @param message    the message to extract from
     * @return the result of the extraction or null if the property was not found
     */
    public Object evaluate(String expression, MuleMessage message)
    {
        Scriptable script = getScript(expression);
        script.setMuleContext(muleContext);
        Bindings bindings = script.getScriptEngine().createBindings();
        script.populateBindings(bindings, message);

        try
        {
            return script.runScript(bindings);
        }
        catch (ScriptException e)
        {
            throw new MuleRuntimeException(e);
        }
        finally {
            bindings.clear();
        }
    }

    protected Scriptable getScript(String expression)
    {
        Scriptable script = (Scriptable)cache.get(expression);
        if (script==null)
        {
            script = new Scriptable(muleContext);
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
