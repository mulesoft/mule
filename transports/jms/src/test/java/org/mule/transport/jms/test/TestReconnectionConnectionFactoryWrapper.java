/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.test;

import java.lang.reflect.Method;
import java.util.List;

import javax.jms.ConnectionFactory;

/**
 * Interface for testing JMS reconnections. Implementing classes should extend a
 * particular JMS providers ConnectionFactory and throw a JMS exception when
 * isEnabled() == false and the mule server is trying to connect to it.
 */
public interface TestReconnectionConnectionFactoryWrapper extends ConnectionFactory
{
    public abstract void init();

    // For InvocationHandler interface
    public abstract Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

    public abstract Object getTargetObject();

    public abstract void setEnabled(boolean enabled);

    public abstract boolean isEnabled();
    
    public abstract List getCalledMethods();
    
    public abstract void closeConnection();
}
