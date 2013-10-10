/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
