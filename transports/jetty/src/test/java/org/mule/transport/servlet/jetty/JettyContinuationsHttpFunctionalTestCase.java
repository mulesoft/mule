/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
