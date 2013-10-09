/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
