/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.resolvers;

import org.mule.umo.lifecycle.Callable;
import org.mule.umo.UMOEventContext;

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

    public Object onCall(UMOEventContext eventContext) throws Exception
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

    public String reflection(Integer integer)
    {
        return "reflection";
    }

}
