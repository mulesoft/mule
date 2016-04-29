/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration.transformer;

public class BindingInterfaceComponent
{

    BindingInterface binding;

    public String invoke(String s)
    {
        s = binding.hello1(s);
        s = binding.hello2(s);
//        s = binding.hello3(s);
//        s = binding.hello4(s);
        return s;
    }

    public void setBinding(BindingInterface binding)
    {
        this.binding = binding;
    }

    public BindingInterface getBinding()
    {
        return binding;

    }

}
