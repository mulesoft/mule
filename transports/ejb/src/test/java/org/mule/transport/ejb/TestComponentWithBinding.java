/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ejb;

import org.mule.container.DummyEjb;

import java.rmi.RemoteException;

/**
 * A test component that uses an EJB binding
 */
public class TestComponentWithBinding
{
    private DummyEjb binding;

    public DummyEjb getBinding()
    {
        return binding;
    }

    public void setBinding(DummyEjb binding)
    {
        this.binding = binding;
    }

    public String process(String data) throws RemoteException
    {
        return binding.reverseString(data);
    }
}
