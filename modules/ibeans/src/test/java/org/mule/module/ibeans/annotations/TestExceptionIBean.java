/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import java.net.UnknownHostException;

import org.ibeans.annotation.Call;
import org.ibeans.annotation.param.UriParam;
import org.ibeans.api.ExceptionListenerAware;

/**
 * A test bean that uses an exception listener rather than declaring exceptions on all the method calls
 */
public interface TestExceptionIBean extends ExceptionListenerAware
{
    @Call(uri = "http://doesnotexist.bom?param={foo}")
    public String doSomething(@UriParam("foo") String foo);

    @Call(uri = "http://doesnotexist.bom")
    public String doSomethingElse() throws UnknownHostException;
}
