/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

public class TestPojo
{
    private String config1;
    private String config2;

    public TestPojo()
    {
    }

    public void method1(String arg1)
    {
    }

    public String method2(String arg1)
    {
        return arg1 + "method2Arg1" + config1 + " ";
    }

    public String method2(String arg1, String arg2)
    {
        return arg1 + arg2 + "method2Arg1Arg2" + config2 + " ";
    }

    public String getConfig1()
    {
        return config1;
    }

    public void setConfig1(String config1)
    {
        this.config1 = config1;
    }

    public String getConfig2()
    {
        return config2;
    }

    public void setConfig2(String config2)
    {
        this.config2 = config2;
    }
}
