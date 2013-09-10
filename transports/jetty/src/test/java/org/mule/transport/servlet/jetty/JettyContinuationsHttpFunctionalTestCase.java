/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.transport.http.functional.HttpFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class JettyContinuationsHttpFunctionalTestCase extends HttpFunctionalTestCase
{
    public JettyContinuationsHttpFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }    
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
           {ConfigVariant.SERVICE, "jetty-continuations-http-functional-test.xml"}            
        });
    }      
}
