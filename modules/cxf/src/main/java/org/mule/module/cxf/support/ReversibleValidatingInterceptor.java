/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;


import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * The ReversibleValidatingInterceptor is going to allow to reset the Xml Stream Reader once it is consumed
 * by the CheckClosingTagsInterceptor to validate the schema.
 */
public class ReversibleValidatingInterceptor extends ReversibleStaxInterceptor
{
    public ReversibleValidatingInterceptor()
    {
        super(Phase.READ);
        addAfter(ProxySchemaValidationInInterceptor.class.getName());
        addAfter(StartBodyInterceptor.class.getName());
    }


}
