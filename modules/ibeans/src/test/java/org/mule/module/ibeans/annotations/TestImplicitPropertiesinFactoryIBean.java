/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.annotation.Call;
import org.ibeans.annotation.param.HeaderParam;
import org.ibeans.api.CallException;
import org.ibeans.api.ExceptionListenerAware;
import org.ibeans.api.ParamFactory;


public interface TestImplicitPropertiesinFactoryIBean extends ExceptionListenerAware
{
    @HeaderParam(value = "Authorization")
    ParamFactory s3SignatureEvaluator = new CheckHTTPPropertiesFactory();

    @Call(uri = "http://s3.amazonaws.com/")
    public Object doStuff() throws CallException;
}
