/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Callable;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.InvocationResult;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;

import java.lang.reflect.Method;

/**
 * An entry-point resolver that only allows Service objects that implement the
 * Callable interface
 *
 * @see org.mule.api.lifecycle.Callable
 */
public class CallableEntryPointResolver implements EntryPointResolver
{
    protected static final Method callableMethod;

    static
    {
        try
        {
            callableMethod = Callable.class.getMethod("onCall", new Class[] {MuleEventContext.class});
        }
        catch (NoSuchMethodException e)
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage("Panic! No onCall(MuleEventContext) method found in the Callable interface."));
        }
    }

    public InvocationResult invoke(Object component, MuleEventContext context) throws Exception
    {
        if (component instanceof Callable)
        {
            Object result = ((Callable) component).onCall(context);
            return new InvocationResult(this, result, callableMethod);
        }
        else
        {
            InvocationResult result = new InvocationResult(this, InvocationResult.State.NOT_SUPPORTED);
            result.setErrorMessage(CoreMessages.objectDoesNotImplementInterface(component, Callable.class).toString());
            return result;
        }
    }

    @Override
    public String toString()
    {
        return "CallableEntryPointResolver{}";
    }
}
