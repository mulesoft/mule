/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.remoting;

import org.mule.util.StringUtils;

import java.io.Serializable;

public class ComplexData implements Serializable
{
    private static final long serialVersionUID = -886414019167115007L;

    private String someString = "Default String";
    private Integer someInteger = new Integer(13);

    public ComplexData()
    {
        super();
    }

    public ComplexData(String someString, Integer someInteger)
    {
        super();
        setSomeString(someString);
        setSomeInteger(someInteger);
    }

    public String toString()
    {
        try
        {
            String currentString = StringUtils.defaultIfEmpty(someString, "NULL");
            return "[ComplexData: [someString=" + currentString + "][someInteger=" + someInteger + "]]";
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Integer getSomeInteger()
    {
        return someInteger;
    }

    public void setSomeInteger(Integer someInteger)
    {
        this.someInteger = someInteger;
    }

    public String getSomeString()
    {
        return someString;
    }

    public void setSomeString(String someString)
    {
        this.someString = someString;
    }

}
