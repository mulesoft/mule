/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.ClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * <code>DynamicEntryPoint</code> is used to detemine the entry point on a
 * bean after an event has been received for it. The entrypoint is then
 * discovered using the event payload type as the argument. An entry point will
 * try and be matched for different argument types so it's possible to have
 * multiple entry points on a single component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class DynamicEntryPoint implements UMOEntryPoint
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(DynamicEntryPoint.class);

    private Map entryPoints = new ConcurrentHashMap();
    private Method currentMethod;

    public Class getParameterType()
    {
        if (currentMethod == null) {
            return null;
        }
        return currentMethod.getParameterTypes()[0];
    }

    /** TODO refactor, extract methods so the logic flow can be comprehended */
    public Object invoke(Object component, UMOEventContext context) throws InvocationTargetException,
            IllegalAccessException, TransformerException
    {
        Object payload = null;

        // Check for method override and remove it from the event
        Object methodOverride = context.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY);
        Method method = null;

        if (component instanceof Callable) {
            method = Callable.class.getMethods()[0];
            payload = context;
        }
        if (method == null) {
            method = getMethod(component, context);
            if (method == null) {
                payload = context.getTransformedMessage();
                method = getMethod(component, payload);
                if (method != null) {
                    RequestContext.rewriteEvent(new MuleMessage(payload, context.getMessage()));
                }
            } else {
                payload = context;
            }
        }

        if (method != null) {
            currentMethod = method;
            if (payload == null) {
                payload = context.getTransformedMessage();
                RequestContext.rewriteEvent(new MuleMessage(payload, context.getMessage()));
            }
            return invokeCurrent(component, payload);
        }

        // Are any methods on the component accepting an context?
        List methods = ClassHelper.getSatisfiableMethods(component.getClass(),
                                                         ClassHelper.getClassTypes(context),
                                                         true,
                                                         true,
                                                         false);
        if (methods.size() > 1) {
            TooManySatisfiableMethodsException tmsmex = new TooManySatisfiableMethodsException(component.getClass());
            throw new InvocationTargetException(
                    tmsmex, "There must be only one method accepting " + context.getClass().getName() +
                    " in component " + component.getClass().getName());
        } else if (methods.size() == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("Dynamic Entrypoint using method: " + component.getClass().getName() + "."
                        + ((Method) methods.get(0)).getName() + "(" + context.getClass().getName() + ")");
            }
            addMethod(component, (Method) methods.get(0), context.getClass());
            return invokeCurrent(component, context);
        }

        /*
         Important! Do not resolve method override if we have nested/chained invocations,
         but use the method from the existing context.
         E.g. Running a Sync LoanBroker application (non-esb) with Axis (org.mule.samples.loanbroker.LoanConsumer)
         produces the following scenario:

          MULE_ENDPOINT property is set to the correct
                vm://localhost/LenderService?method=setLenderList
         , while SOAPAction has a value of
                SOAPAction=http://localhost:18080/mule/CreditAgencyService?method=getCreditProfile
          At the same time, the 'method' (MuleProperties.MULE_METHOD_PROPERTY) STILL points to 'getCreditProfile'
          Thus, when we are processing the invocation, the getCreditProfile gets called which is incorrect.
        */
        if (currentMethod == null) {

            if (methodOverride instanceof Method) {
                method = (Method) methodOverride;
            } else if (methodOverride != null) {
                payload = context.getTransformedMessage();
                if (logger.isDebugEnabled()) {
                    logger.debug("Manual method resolution requested, methodOverride=" + methodOverride);
                }


                //Get the method that matches the method name with the current argument types
                method = ClassHelper.getMethod(methodOverride.toString(), ClassHelper.getClassTypes(payload), component.getClass());
                if (logger.isDebugEnabled()) {
                    logger.debug("Method override resolved to " + method);
                }
                if (method == null) {
                    NoSatisfiableMethodsException nsmex = new NoSatisfiableMethodsException(component.getClass());
                    throw new InvocationTargetException(nsmex,
                            "Manually overridden method '" + methodOverride +
                            "' not found in " + component.getClass().getName());
                }
            }
        }

        if (method != null) {
            currentMethod = method;
            if (payload == null) {
                payload = context.getTransformedMessage();
                // Need to clear the method override by that point
                //context.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY);
                RequestContext.rewriteEvent(new MuleMessage(payload, context.getMessage()));
            }
            return invokeCurrent(component, payload);
        }

        methods = ClassHelper.getSatisfiableMethods(component.getClass(),
                                                    ClassHelper.getClassTypes(payload),
                                                    true,
                                                    true,
                                                    true);
        if (methods.size() > 1) {
            TooManySatisfiableMethodsException tmsmex = new TooManySatisfiableMethodsException(component.getClass());
            throw new InvocationTargetException(
                    tmsmex, "There must be only one method accepting " + payload.getClass().getName() +
                    " in component " + component.getClass().getName());
        }
        if (methods.size() == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("Dynamic Entrypoint using method: " + component.getClass().getName() + "."
                        + ((Method) methods.get(0)).getName() + "(" + payload.getClass().getName() + ")");
            }
            addMethod(component, (Method) methods.get(0), payload.getClass());
            return invokeCurrent(component, payload);
        } else {
            NoSatisfiableMethodsException e = new NoSatisfiableMethodsException(component.getClass());
            throw new InvocationTargetException(e, "Failed to find entry point for component: "
                    + component.getClass().getName() + " with argument: " + payload.getClass().getName());
        }
    }

    protected Method getMethod(Object component, Object arg)
    {
        return (Method) entryPoints.get(component.getClass().getName() + ":" + arg.getClass().getName());
    }

    protected void addMethod(Object component, Method method, Class arg)
    {
        entryPoints.put(component.getClass().getName() + ":" + arg.getName(), method);
        currentMethod = method;
    }

    /**
     * Will invoke the entry point method on the given component
     * 
     * @param component the component to invoke
     * @param arg the argument to pass to the method invocation
     * @return An object (if any) returned by the invocation
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object invokeCurrent(Object component, Object arg) throws InvocationTargetException, IllegalAccessException
    {
        String methodCall = null;
        if (logger.isDebugEnabled()) {
            methodCall = component.getClass().getName() + "." + currentMethod.getName() + "("
                    + arg.getClass().getName() + ")";
            logger.debug("Invoking " + methodCall);
        }

        Object[] args;
        if (arg.getClass().isArray()) {
            if(Object[].class.isAssignableFrom(arg.getClass())) {
                args = (Object[]) arg;
            } else {
                args = new Object[]{arg};
            }
        } else if (arg instanceof NullPayload) {
            args = null;
        } else {
            args = new Object[] { arg };
        }
        Object result = currentMethod.invoke(component, args);
        if (logger.isDebugEnabled()) {
            logger.debug("Result of call " + methodCall + " is " + (result == null ? "null" : "not null"));
        }
        return result;
    }

    public boolean isVoid()
    {
        if (currentMethod == null) {
            return false;
        }
        return currentMethod.getReturnType().getName().equals("void");
    }

    public String getMethodName()
    {
        if (currentMethod == null) {
            return null;
        }
        return currentMethod.getName();
    }
}
