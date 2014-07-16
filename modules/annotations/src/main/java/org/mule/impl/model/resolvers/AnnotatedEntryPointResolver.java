/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.InvocationResult;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.expression.ExpressionAnnotationsHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.model.resolvers.AbstractEntryPointResolver;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.cglib.proxy.Enhancer;

/**
 * A Mule {@link org.mule.api.model.EntryPointResolver} implementation that will resolve methods on a service class
 * that have Mule expression annotations such as {@link org.mule.api.annotations.param.Payload}, {@link org.mule.api.annotations.param.InboundHeaders}
 * It will transform the received message to match the annotated method arguments. For example -
 * <code>
 * public Object doSomething(
 *         &#64;XPath ("/foo/bar") String bar,
 *         &#64;Payload Document doc,
 *         &#64;InboundHeaders("name") String name)
 *  {
 *      //do stuff
 *  }
 * </code>
 *
 * The component method above will be invoked by running the XPath expression on the payload, adding a second parameter as
 * the payload itself and passing in the header 'name' as the third parameter.
 *
 * There are some rules for how components with annotations will be processed -
 * <ul>
 * <li>For components with more than one method annotated with Mule expression annotations the method name to call needs
 * to be set on the incoming message or endpoint using the {@link org.mule.api.config.MuleProperties#MULE_METHOD_PROPERTY} key.</li>
 * <li>If the component has only one method annotated with Mule expression annotations there is no need to set the method name to invoke</li>
 * <li>Every parameter on the method needs to be annotated</li>
 * </ul>
 *
 * @see org.mule.api.annotations.param.Payload
 * @see org.mule.api.annotations.param.InboundHeaders
 * @see org.mule.api.annotations.param.InboundAttachments
 * @see org.mule.api.annotations.param.OutboundHeaders
 * @see org.mule.api.annotations.param.OutboundAttachments
 * @see org.mule.api.annotations.expressions.Mule
 *
 * @since 3.0
 *
 */
public class AnnotatedEntryPointResolver extends AbstractEntryPointResolver
{
    private AtomicBoolean cacheBuilt = new AtomicBoolean(false);

    private Map<Method, Transformer> transformerCache = new ConcurrentHashMap<Method, Transformer>();

    @Override
    public InvocationResult invoke(Object component, MuleEventContext context) throws Exception
    {
        try
        {
            initCache(component, context);
        }
        catch (Exception e)
        {
            InvocationResult result = new InvocationResult(this, InvocationResult.State.NOT_SUPPORTED);
            result.setErrorMessage(e.toString());
            return result;
        }

        ConcurrentHashMap<String, Method> methodCache = getMethodCache(component);
        if (methodCache.size() == 0)
        {
            InvocationResult result = new InvocationResult(this, InvocationResult.State.NOT_SUPPORTED);
            result.setErrorMessage("Component: " + component + " doesn't have any annotated methods, skipping.");
            return result;
        }

        Object[] payload;
        Method method = null;
        //We remove the property here as a workaround to MULE-4769
        Object tempMethod = context.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY, PropertyScope.INVOCATION);
        String methodName = null;
        if (tempMethod != null && tempMethod instanceof Method)
        {
            method = (Method) tempMethod;
        }
        else
        {
            methodName = (String)tempMethod;
        }

        //If a method param is set use that over anything else. This is used by the @Reply Callbacks and where annotations are set
        //on the method
        if (methodName != null)
        {
            method = getMethodByName(component, methodName, context);
            if (method == null)
            {
                InvocationResult result = new InvocationResult(this, InvocationResult.State.NOT_SUPPORTED);
                result.setErrorMessage("Method not found: " + methodName + " on object: " + component.getClass() + ". If the component is a proxy there needs to be an interface on the proxy that defines this method");
                return result;
                //TODO i18n
            }
            payload = getPayloadForMethod(method, component, context);
        }
        else if (method != null)
        {
            payload = getPayloadForMethod(method, component, context);
        }
        else if (methodCache.size() == 1)
        {
            method = methodCache.values().iterator().next();
            payload = getPayloadForMethod(method, component, context);
        }
        else
        {
            InvocationResult result = new InvocationResult(this, InvocationResult.State.FAILED);
            result.setErrorMessage("Component: " + component + " has more than one method annotated, which means the target method name needs to be set on the event");
            return result;
        }
        return invokeMethod(component, method,
                (method.getParameterTypes().length == 0 ? ClassUtils.NO_ARGS : payload));
    }

    protected Object[] getPayloadForMethod(Method method, Object component, MuleEventContext context) throws TransformerException, InitialisationException
    {
        Object[] payload = null;
        Method m = method;
        //If we are using cglib enhanced service objects, we need to read annotations from the real component class
        if (Enhancer.isEnhanced(component.getClass()))
        {
            try
            {
                m = component.getClass().getSuperclass().getMethod(method.getName(), method.getParameterTypes());
            }
            catch (NoSuchMethodException e)
            {
                throw new TransformerException(CoreMessages.createStaticMessage(e.getMessage()), e);
            }
        }

        if (AnnotationUtils.getParamAnnotationsWithMeta(m, Evaluator.class).size() > 0)
        {
            payload = getPayloadFromMessageWithAnnotations(m, context);
        }
        return payload;
    }

    protected Object[] getPayloadFromMessageWithAnnotations(Method method, MuleEventContext context) throws TransformerException, InitialisationException
    {
        ExpressionTransformer trans = (ExpressionTransformer) transformerCache.get(method);
        if (trans == null)
        {
            trans = ExpressionAnnotationsHelper.getTransformerForMethodWithAnnotations(method, context.getMuleContext());
            transformerCache.put(method, trans);
        }

        Object o = trans.transform(context.getMessage());
        if (o instanceof NullPayload)
        {
            return new Object[]{null};
        }
        else if (o.getClass().isArray())
        {
            return (Object[]) o;
        }
        else
        {
            return new Object[]{o};
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("AnnotatedEntryPointResolver");
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }


    protected void initCache(Object component, MuleEventContext context)
    {
        if (!cacheBuilt.get())
        {
            synchronized (this)
            {
                try
                {
                    if (!cacheBuilt.get())
                    {
                        for (int i = 0; i < component.getClass().getMethods().length; i++)
                        {
                            Method method = component.getClass().getMethods()[i];
                            if (AnnotationUtils.getParamAnnotationsWithMeta(method, Evaluator.class).size() > 0)
                            {
                                this.addMethodByName(component, method, context);
                            }
                        }
                    }
                }
                finally
                {
                    cacheBuilt.set(true);
                }
            }
        }
    }
}
