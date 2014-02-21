/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleContext;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.ExceptionUtils;

import java.net.BindException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpSamePortTestCase
{

    public static final String PATH_PROPERTY = "path";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void twoApplicationCannotUseSamePort() throws Throwable
    {
        MuleContext firstMuleContext = null;
        MuleContext secondMuleContext = null;
        try
        {
            firstMuleContext = buildApp("helloWorld");
            thrown.expect(BindException.class);
            secondMuleContext = buildApp("helloMule");
        }
        catch (Exception e)
        {
            throw ExceptionUtils.getRootCause(e);
        }
        finally
        {
            disposeQuietly(firstMuleContext);
            disposeQuietly(secondMuleContext);
        }
    }

    private void disposeQuietly(MuleContext muleContext)
    {
        try
        {
            muleContext.dispose();
        }
        catch (Exception e)
        {
            //Nothing to do.
        }
    }

    private MuleContext buildApp(String path) throws Exception
    {
        System.setProperty(PATH_PROPERTY, path);
        try
        {
            return new ApplicationContextBuilder().setApplicationResources(new String[] {"http-same-port-config.xml"}).build();
        }
        finally
        {
            System.clearProperty(PATH_PROPERTY);
        }
    }
}
