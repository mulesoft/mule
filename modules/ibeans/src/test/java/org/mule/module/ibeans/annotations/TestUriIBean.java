/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import java.net.UnknownHostException;

import org.ibeans.annotation.Call;
import org.ibeans.annotation.Template;
import org.ibeans.annotation.param.Optional;
import org.ibeans.annotation.param.UriParam;
import org.ibeans.api.ExceptionListenerAware;

/**
 * A test bean that uses an exception listener rather than declaring exceptions on all the method calls
 */

public interface TestUriIBean extends ExceptionListenerAware
{
    @UriParam("do_something_uri")
    public static final String DO_SOMETHING_URI = "doesnotexist.bom?param1=";

    @Template("http://{do_something_uri}{foo}")
    public String doSomething(@UriParam("foo") String foo);

    @Template("http://{do_something_uri}{foo}&param2={bar}")
    public String doSomethingElse(@UriParam("foo") String foo, @UriParam("bar") String bar) throws UnknownHostException;

    @Call(uri = "http://{do_something_uri}")
    public String doSomethingNoParams() throws Exception;

    @Template("http://{do_something_uri}{foo}&param2={bar}")
    public String doSomethingOptional(@Optional @UriParam("foo") String foo, @Optional @UriParam("bar") String bar) throws UnknownHostException;
}
