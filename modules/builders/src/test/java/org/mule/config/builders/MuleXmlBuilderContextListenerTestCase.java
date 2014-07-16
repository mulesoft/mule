/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FilenameUtils;

import java.io.File;

import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;

public class MuleXmlBuilderContextListenerTestCase extends AbstractMuleTestCase
{
    private MuleXmlBuilderContextListener listener;
    private ServletContext context;

    @Before
    public void setUp() throws Exception
    {
        listener = new MuleXmlBuilderContextListener();
        context = mock(ServletContext.class);
    }

    @After
    public void tearDown() throws Exception
    {
        listener.muleContext.stop();
    }

    @Test
    public void noMuleAppProperties()
    {
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG))
            .thenReturn("mule-config.xml");
        when(context.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(null);
        when(context.getAttribute(MuleXmlBuilderContextListener.ATTR_JAVAX_SERVLET_CONTEXT_TEMPDIR))
            .thenReturn(new File(".mule/testWeb"));

        listener.initialize(context);

        verify(context).getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG);
        verify(context).getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        
        assertEquals("./.mule/testWeb", listener.muleContext.getConfiguration().getWorkingDirectory());
    }

    @Test
    public void withImplicitMuleAppProperties()
    {
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG))
            .thenReturn("org/mule/config/builders/mule-config.xml");
        when(context.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(null);
        when(context.getAttribute(MuleXmlBuilderContextListener.ATTR_JAVAX_SERVLET_CONTEXT_TEMPDIR))
                .thenReturn(new File(".mule/testWeb"));

        listener.initialize(context);

        verify(context).getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG);
        verify(context).getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

        // TODO don't like this convention, the whole mule-app.properties WAR support in Mule 3 is redundant
        // and should go away
        assertWorkingDirectoryEndsWith("target/.appTmp/testWeb");
    }

    @Test
    public void withExplicitMuleAppProperties()
    {
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG))
            .thenReturn("org/mule/config/builders/mule-config.xml");
        when(context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_APP_CONFIG))
        .thenReturn("org/mule/config/builders/mule-app-ppp.properties");
        when(context.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(null);
        when(context.getAttribute(MuleXmlBuilderContextListener.ATTR_JAVAX_SERVLET_CONTEXT_TEMPDIR))
                .thenReturn(new File(".mule/testWeb"));

        listener.initialize(context);

        verify(context).getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG);
        verify(context).getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

        // TODO don't like this convention, the whole mule-app.properties WAR support in Mule 3 is redundant
        // and should go away
        assertWorkingDirectoryEndsWith("target/.appTmp2/testWeb");
    }

    private void assertWorkingDirectoryEndsWith(String expected)
    {
        // handle Windows filenames, just in case
        String workingDirectory = listener.muleContext.getConfiguration().getWorkingDirectory().replace('\\', '/');
        workingDirectory = FilenameUtils.separatorsToUnix(workingDirectory);
        assertTrue(workingDirectory.endsWith(expected));
    }
}
