/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
