/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

/**
 * TODO
 */
public class DummyComponentWithBinding
{
    private DummyBinding binding;

    public Object process(String data)
    {
        return binding.doSomething(data);
    }

    public DummyBinding getBinding()
    {
        return binding;
    }

    public void setBinding(DummyBinding binding)
    {
        this.binding = binding;
    }
}
