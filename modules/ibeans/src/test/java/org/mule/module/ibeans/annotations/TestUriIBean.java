/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
