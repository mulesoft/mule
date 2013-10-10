/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
