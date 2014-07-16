/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import java.util.Arrays;

/**
 * Test bean for testing JSON
 */
public class TestBean
{

    private String name;
    private int id;
    private double doublev;
    private char[] options;
    private String func1;

    public TestBean()
    {
        super();
    }

    public TestBean(String name, int id, double doublev, String func1)
    {
        this.name = name;
        this.id = id;
        this.doublev = doublev;
        this.func1 = func1;
    }

    public double getDoublev()
    {
        return doublev;
    }

    public void setDoublev(double doublev)
    {
        this.doublev = doublev;
    }

    public String getFunc1()
    {
        return func1;
    }

    public void setFunc1(String func1)
    {
        this.func1 = func1;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public char[] getOptions()
    {
        return options;
    }

    public void setOptions(char[] options)
    {
        this.options = options;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        TestBean testBean = (TestBean) o;

        if (id != testBean.id)
        {
            return false;
        }
        if (func1 != null ? !func1.equals(testBean.func1) : testBean.func1 != null)
        {
            return false;
        }
        if (name != null ? !name.equals(testBean.name) : testBean.name != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        result = 31 * result + id;
        temp = doublev != +0.0d ? Double.doubleToLongBits(doublev) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (options != null ? Arrays.hashCode(options) : 0);
        result = 31 * result + (func1 != null ? func1.hashCode() : 0);
        return result;
    }
}
