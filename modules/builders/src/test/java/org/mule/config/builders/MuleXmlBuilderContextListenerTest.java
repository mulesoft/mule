/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;

import junit.framework.TestCase;

import org.springframework.web.context.WebApplicationContext;

public class MuleXmlBuilderContextListenerTest extends TestCase
{

    private MuleXmlBuilderContextListener listener;
    private ServletContext context;

    public void setUp() throws Exception
    {
        super.setUp();
        listener = new MuleXmlBuilderContextListener();
        context = mock(ServletContext.class);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        listener.muleContext.stop();
    }

    public void testNoMuleAppProperties()
    {
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG))
            .thenReturn("mule-config.xml");
        when(context.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(null);

        listener.initialize(context);

        verify(context).getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG);
        verify(context).getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

        assertEquals("./.mule", listener.muleContext.getConfiguration().getWorkingDirectory());
    }

    public void testWithImplicitMuleAppProperties()
    {
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG))
            .thenReturn("org/mule/config/builders/mule-config.xml");
        when(context.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(null);

        listener.initialize(context);

        verify(context).getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG);
        verify(context).getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

        assertTrue(listener.muleContext.getConfiguration().getWorkingDirectory().endsWith(
            "target/.appTmp"));
    }

    public void testWithExplicitMuleAppProperties()
    {
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG))
            .thenReturn("org/mule/config/builders/mule-config.xml");
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_APP_CONFIG))
        .thenReturn("org/mule/config/builders/mule-app-ppp.properties");
        when(context.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(null);

        listener.initialize(context);

        verify(context).getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG);
        verify(context).getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

        assertTrue(listener.muleContext.getConfiguration().getWorkingDirectory().endsWith(
            "target/.appTmp2"));
    }
}
