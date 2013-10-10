/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
