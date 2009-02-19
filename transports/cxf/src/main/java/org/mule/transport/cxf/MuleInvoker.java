/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.ServiceException;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.NullPayload;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * Invokes a Mule Service via a CXF binding.
 */
public class MuleInvoker implements Invoker
{
    private final CxfMessageReceiver receiver;
    private final boolean synchronous;
    private Class<?> targetClass;
    
    public MuleInvoker(CxfMessageReceiver receiver, Class<?> targetClass, boolean synchronous)
    {
        this.receiver = receiver;
        this.targetClass = targetClass;
        this.synchronous = synchronous;
    }

    public Object invoke(Exchange exchange, Object o)
    {
        
        MuleMessage message = null;
        try
        {
            MuleMessage reqMsg = (MuleMessage) exchange.getInMessage().get(CxfConstants.MULE_MESSAGE);
            CxfMessageAdapter messageAdapter = (CxfMessageAdapter) receiver.getConnector().getMessageAdapter(
                reqMsg);
            messageAdapter.setPayload(exchange.getInMessage());

            BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);
            Service svc = exchange.get(Service.class);
            
            if (!receiver.isProxy())
            {
                MethodDispatcher md = (MethodDispatcher) svc.get(MethodDispatcher.class.getName());
                Method m = md.getMethod(bop);
                if (targetClass != null)
                {
                    m = matchMethod(m, targetClass);
                }
            
                messageAdapter.setProperty(MuleProperties.MULE_METHOD_PROPERTY, m);
            }
            
            DefaultMuleMessage muleReq = new DefaultMuleMessage(messageAdapter);
            
            if (bop != null)
            {
                muleReq.setProperty(CxfConstants.INBOUND_OPERATION, bop.getOperationInfo().getName(), PropertyScope.INVOCATION);
                muleReq.setProperty(CxfConstants.INBOUND_SERVICE, svc.getName(), PropertyScope.INVOCATION);
            }
            
            String replyTo = (String) exchange.getInMessage().get(MuleProperties.MULE_REPLY_TO_PROPERTY);
            if (replyTo != null)
            {
                muleReq.setReplyTo(replyTo);
            }
            
            String corId = (String) exchange.getInMessage().get(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
            if (corId != null)
            {
                muleReq.setCorrelationId(corId);
            }

            String corGroupSize = (String) exchange.getInMessage().get(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
            if (corGroupSize != null)
            {
                muleReq.setCorrelationGroupSize(Integer.valueOf(corGroupSize));
            }

            String corSeq = (String) exchange.getInMessage().get(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
            if (corSeq != null)
            {
                muleReq.setCorrelationSequence(Integer.valueOf(corSeq));
            }
            
            message = receiver.routeMessage(muleReq, synchronous);
        }
        catch (MuleException e)
        {
            throw new Fault(e);
        }

        if (message != null)
        {
            if (message.getExceptionPayload() != null)
            {
                Throwable cause = message.getExceptionPayload().getException();
                if (cause instanceof ServiceException)
                {
                    cause = cause.getCause();
                }

                exchange.getInMessage().put(FaultMode.class, FaultMode.UNCHECKED_APPLICATION_FAULT);
                if (cause instanceof Fault)
                {
                    throw (Fault) cause;
                }

                throw new Fault(cause);
            }
            else if (message.getPayload() instanceof NullPayload)
            {
                return new MessageContentsList((Object)null);
            }
            else if (receiver.isProxy())
            {
                message.getPayload();
                return new Object[] { message };
            }
            else
            {
                return new Object[]{message.getPayload()};
            }
        }
        else
        {
            return new MessageContentsList((Object)null);
        }
    }

    public InboundEndpoint getEndpoint()
    {
        return receiver.getEndpoint();
    }

    /**
     * Returns a Method that has the same declaring class as the class of
     * targetObject to avoid the IllegalArgumentException when invoking the
     * method on the target object. The methodToMatch will be returned if the
     * targetObject doesn't have a similar method.
     * 
     * @param methodToMatch The method to be used when finding a matching method
     *            in targetObject
     * @param targetObject The object to search in for the method.
     * @return The methodToMatch if no such method exist in the class of
     *         targetObject; otherwise, a method from the class of targetObject
     *         matching the matchToMethod method.
     */
    private static Method matchMethod(Method methodToMatch, Class<?> targetClass) {
        Class<?>[] interfaces = targetClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            Method m = getMostSpecificMethod(methodToMatch, interfaces[i]);
            if (!methodToMatch.equals(m)) {
                return m;
            }
        }
        return methodToMatch;
    }

    /**
     * Return whether the given object is a J2SE dynamic proxy.
     * 
     * @param object the object to check
     * @see java.lang.reflect.Proxy#isProxyClass
     */
    public static boolean isJdkDynamicProxy(Object object) {
        return object != null && Proxy.isProxyClass(object.getClass());
    }

    /**
     * Given a method, which may come from an interface, and a targetClass used
     * in the current AOP invocation, find the most specific method if there is
     * one. E.g. the method may be IFoo.bar() and the target class may be
     * DefaultFoo. In this case, the method may be DefaultFoo.bar(). This
     * enables attributes on that method to be found.
     * 
     * @param method method to be invoked, which may come from an interface
     * @param targetClass target class for the curren invocation. May be
     *            <code>null</code> or may not even implement the method.
     * @return the more specific method, or the original method if the
     *         targetClass doesn't specialize it or implement it or is null
     */
    public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
        if (method != null && targetClass != null) {
            try {
                method = targetClass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                // Perhaps the target class doesn't implement this method:
                // that's fine, just use the original method
            }
        }
        return method;
    }
}
