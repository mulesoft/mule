/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

public class Target implements Callable
{

    public String array(String[] array)
    {
        return "array";
    }

    public String object(Object object)
    {
        return "object";
    }

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        return "callable";
    }

    public String custom(Object object)
    {
        return "custom";
    }

    public String methodString(String string)
    {
        return "methodString";
    }

    public String methodInteger(Integer integer)
    {
        return "methodInteger";
    }

    public String noArguments()
    {
        return "noArguments";
    }

    public String property(Object object)
    {
        return "property";
    }

    public String reflection(Integer integer, String string)
    {
        return "reflection";
    }

}
