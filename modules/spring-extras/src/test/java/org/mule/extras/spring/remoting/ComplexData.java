/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.remoting;

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
