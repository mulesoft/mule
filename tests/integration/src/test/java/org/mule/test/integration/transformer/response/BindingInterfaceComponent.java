/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transformer.response;

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
