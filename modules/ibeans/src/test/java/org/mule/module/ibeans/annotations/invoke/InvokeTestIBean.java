/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations.invoke;

import org.ibeans.annotation.Invoke;
import org.ibeans.annotation.param.PropertyParam;

/**
 * TODO
 */

public interface InvokeTestIBean
{
    @PropertyParam("dummy")
    public static final HelloParamFactory hello = new HelloParamFactory();

    @Invoke(object = "dummy", method = "sayHello")
    public String greet(@PropertyParam("name") String name) throws Exception;

    @Invoke(object = "dummy2", method = "sayHello")
    public String greetFail1(@PropertyParam("name") String name) throws Exception;

    @Invoke(object = "dummy", method = "sayHellox")
    public String greetFail2(@PropertyParam("name") String name) throws Exception;

    @Invoke(object = "dummy", method = "sayHello")
    public String greetFail3(@PropertyParam("name") String name, @PropertyParam("location") String location) throws Exception;
}
