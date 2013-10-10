/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import org.mule.api.MuleMessage;

import org.ibeans.annotation.State;
import org.ibeans.annotation.Template;
import org.ibeans.annotation.param.HeaderParam;
import org.ibeans.annotation.param.Order;
import org.ibeans.annotation.param.PropertyParam;
import org.ibeans.annotation.param.UriParam;
import org.ibeans.api.CallException;
import org.ibeans.api.ExceptionListenerAware;
import org.ibeans.api.ParamFactory;
import org.ibeans.api.Response;

public interface TestParamsFactoryIBean extends ExceptionListenerAware
{
    @UriParam("param1")
    @Order(1)
    public static final FirstParamFactory FIRST_URI_FACTORY = new FirstParamFactory();

    @UriParam("param2")
    @Order(2)
    public static final SecondParamFactory SECOND_URI_FACTORY = new SecondParamFactory();

    @HeaderParam("header1")
    @Order(3)
    public static final FirstParamFactory FIRST_HEADER_FACTORY = new FirstParamFactory();

    @HeaderParam("header2")
    @Order(4)
    public static final SecondParamFactory SECOND_HEADER_FACTORY = new SecondParamFactory();

    @State
    void init(@PropertyParam("key") byte[] key);

    @Template("The key is {param1} for {foo}. Param2 is: '{param2}'")
    public String doUriParams(@UriParam("foo") String foo) throws CallException;

    @Template("The key is {paramX} for {foo}. Param2 is: '{param2}'")
    public String doMethodUriParam(@UriParam("foo") String foo, @UriParam("paramX") ParamFactory factory) throws CallException;

    @Template("Value is: {foo}")
    public MuleMessage doHeaderParam(@UriParam("foo") String foo) throws CallException;

    @Template("Value is: {foo}")
    public MuleMessage doMethodHeaderParam(@UriParam("foo") String foo, @HeaderParam("echoHeader") EchoParamFactory factory) throws CallException;

    @Template("Value is: {foo}")
    public MuleMessage doMethodPropertyParam(@UriParam("foo") String foo, @PropertyParam("customProperty") String prop, @HeaderParam("propHeader") ReversePropertyParamFactory factory) throws CallException;

    @Template("Foo")
    public MuleMessage doTestHeadersWithNoParams() throws CallException;

    @Template("Foo")
    public Response doTestHeadersWithNoParamsAndResponse() throws CallException;

    @Template("Value is: {foo}")
    public Response doHeaderParamAndResponse(@UriParam("foo") String foo) throws CallException;

    @Template("Value is: {foo}")
    public Response doMethodHeaderParamAndResponse(@UriParam("foo") String foo, @HeaderParam("echoHeader") EchoParamFactory factory) throws CallException;

    @Template("Value is: {foo}")
    public Response doMethodPropertyParamAndResponse(@UriParam("foo") String foo, @PropertyParam("customProperty") String prop, @HeaderParam("propHeader") ReversePropertyParamFactory factory) throws CallException;
}
