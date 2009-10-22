/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.resolvers;

import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.InvocationResult;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.annotations.Entrypoint;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.model.resolvers.AbstractEntryPointResolver;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.utils.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * A Mule {@link org.mule.api.model.EntryPointResolver} implementation that will resolve methods on a service class
 * that has the {@link org.mule.config.annotations.Entrypoint} annotation. It will transform the received message to
 * match the arguments on the annotated method and will honor any parameter annotations such as {@link org.mule.config.annotations.expressions.XPath}
 * or {@link org.mule.config.annotations.expressions.Mule} annotations.
 */
public class AnnotatedEntryPointResolver extends AbstractEntryPointResolver implements MuleContextAware
{
    private Set<String> ignoredMethods = new HashSet<String>(Arrays.asList("equals",
            "getInvocationHandler", "set*", "toString",
            "getClass", "notify", "notifyAll", "wait", "hashCode", "clone", "is*", "get*"));

    private volatile boolean firstTime = true;

    private Map<Method, Transformer> transformerCache = new ConcurrentHashMap();

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public InvocationResult invoke(Object component, MuleEventContext context) throws Exception
    {
        if (firstTime)
        {
            try
            {
                initCache(component, context);
            }
            catch (Exception e)
            {
                InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
                result.setErrorMessage(e.toString());
                return result;
            }
        }

        Object[] payload;
        Method method;
        String methodName = (String) context.getMessage().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        //If a method param is set use that over anything else. This is used by the @Reply Callbacks and where annotations are set
        //on the method
        if (methodName != null)
        {
            method = getMethodByName(methodName, component.getClass());
            if (method == null)
            {
                //TODO i18n
                throw new IllegalArgumentException("Method not found: " + methodName + " on object: " + component.getClass());
            }
            payload = getPayloadForMethod(method, component, context);
        }
        else if (methodCache.size() == 1)
        {
            method = (Method) methodCache.values().iterator().next();
            payload = getPayloadForMethod(method, component, context);
        }
        else
        {
            payload = getPayloadFromMessage(context);

            method = getMethodByArguments(component, payload);

            if (method == null)
            {
                InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
                result.setErrorMessage("@Entrypoint annotation not set on any methods of the service component: " + component);
                return result;
            }
        }
        return invokeMethod(component, method,
                (method.getParameterTypes().length == 0 ? ClassUtils.NO_ARGS : payload));
    }

    protected Object[] getPayloadForMethod(Method method, Object component, MuleEventContext context) throws TransformerException, InitialisationException
    {
        Object[] payload;
        if (AnnotationUtils.methodHasParamAnnotations(method))
        {
            payload = getPayloadFromMessageWithAnnotations(method, context);
        }
        else
        {
            payload = getPayloadFromMessage(context);
            List methods = ClassUtils.getSatisfiableMethods(component.getClass(), ClassUtils.getClassTypes(payload), true, true, ignoredMethods);
            if (methods.size() == 0 && method.getParameterTypes().length == 1)
            {
                Object temp = context.getMessage().getPayload(method.getParameterTypes()[0]);
                payload = new Object[]{temp};
            }
        }
        return payload;
    }

    protected Object[] getPayloadFromMessageWithAnnotations(Method method, MuleEventContext context) throws TransformerException, InitialisationException
    {
        ExpressionTransformer trans = (ExpressionTransformer) transformerCache.get(method);
        if (trans == null)
        {
            trans = AnnotationUtils.getTransformerForMethodWithAnnotations(method, muleContext);
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

    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("AnnotatedEntryPointResolver");
        sb.append("{transformFirst=").append(isTransformFirst());
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }

    protected synchronized void initCache(Object component, MuleEventContext context)
    {

        for (int i = 0; i < component.getClass().getMethods().length; i++)
        {
            Method method = component.getClass().getMethods()[i];
            if (method.isAnnotationPresent(Entrypoint.class))
            {
                this.addMethodByName(method, context);
            }
        }
        firstTime = false;
//        if (methodCache.size() == 0)
//        {
//            throw new IllegalStateException(AnnotationsMessages.serviceHasNoEntrypoint(component.getClass()).getMessage());
//        }
    }

    protected Method getMethodByName(String name, Class clazz)
    {
        Method m = null;
        for (int i = 0; i < clazz.getMethods().length; i++)
        {
            m = clazz.getMethods()[i];
            if (m.getName().equals(name))
            {
                break;
            }
        }
        return m;
    }
}
