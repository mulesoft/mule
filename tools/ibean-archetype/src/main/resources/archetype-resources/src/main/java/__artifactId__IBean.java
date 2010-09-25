#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * ${symbol_dollar}Id: ${symbol_dollar}
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package ${package};

import org.ibeans.annotation.Call;
import org.ibeans.annotation.Template;
import org.ibeans.api.CallException;
import org.ibeans.annotation.State;
import org.ibeans.annotation.Usage;
import org.ibeans.annotation.filter.JsonErrorFilter;
import org.ibeans.annotation.filter.XmlErrorFilter;
import org.ibeans.annotation.param.Optional;
import org.ibeans.annotation.param.BodyParam;
import org.ibeans.annotation.param.ReturnType;
import org.ibeans.annotation.param.UriParam;


@Usage("How to use this bean")
public interface ${artifactId}IBean // Add if you need Http Basic Authentication extends HttpBasicAuthentication
{
    //TODO Declare default values like this
    @UriParam("foo")
    public static final String DEFAULT_FOO = "bar";

    //TODO State calls allow you configure common values
    @State
    public void init(@UriParam("foo") String defaultFoo);

    //TODO Add one or more call methods that communicate with your service
    //NOTE the Template parameter is used to evaluate a string value, it's used here so the OOTB testcase works
    @Template("http://www.foo.com/update/{foo}")
    public String updateFoo(@BodyParam("foo") String value) throws CallException;

    //@Call(uri = "http://www.foo.com/update/{foo}")
    //public String defaultUpdateFoo() throws CallException;
}
