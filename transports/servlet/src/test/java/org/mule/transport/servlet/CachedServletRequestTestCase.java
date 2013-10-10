/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CachedServletRequestTestCase extends AbstractMuleContextTestCase
{
    private static final String CONNECTOR_NAME = "ServletConnector";

    private ServletContext mockServletContext;
    private ServletConfig mockServletConfig;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getAttribute(eq(MuleProperties.MULE_CONTEXT_PROPERTY))).thenReturn(muleContext);

        mockServletConfig = mock(ServletConfig.class);
        when(mockServletConfig.getServletContext()).thenReturn(mockServletContext);
        when(mockServletConfig.getInitParameter(eq(AbstractReceiverServlet.SERVLET_CONNECTOR_NAME_PROPERTY))).thenReturn(CONNECTOR_NAME);
    }

    @Test
    public void testReceiverServletUsingCachedServletRequest() throws Exception
    {
        registerServletConnector();

        SensingMuleReceiverServlet servlet = new SensingMuleReceiverServlet();
        servlet.init(mockServletConfig);

        HttpServletRequest mockRequest = createMockRequest();
        servlet.doGet(mockRequest, null);
        assertTrue(servlet.isUsingCachedServletRequest());
    }

    private void registerServletConnector() throws MuleException
    {
        ServletConnector connector = new ServletConnector(muleContext);
        connector.setName(CONNECTOR_NAME);
        connector.setUseCachedHttpServletRequest(true);
        muleContext.getRegistry().registerConnector(connector);
    }

    private HttpServletRequest createMockRequest() throws IOException
    {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream());
        when(mockRequest.getPathInfo()).thenReturn("/foo");
        return mockRequest;
    }

    private static class SensingMuleReceiverServlet extends MuleReceiverServlet
    {
        private boolean usingCachedServletRequest = false;

        public SensingMuleReceiverServlet()
        {
            super();
        }

        @Override
        protected MessageReceiver getReceiverForURI(HttpServletRequest request)
            throws EndpointException
        {
            usingCachedServletRequest = (request instanceof CachedHttpServletRequest);

            // throw an exception here to bypass all the code dealing with Mule internals
            // (i.e. message receiver, endpoints, routing messages etc.). We have had access to
            // the object we wanted to verify and that's good enough.
            throw new AbortControlFlowException();
        }

        @Override
        protected void handleException(Throwable exception, String message, HttpServletResponse response)
        {
            assertTrue(exception instanceof AbortControlFlowException);
        }

        public boolean isUsingCachedServletRequest()
        {
            return usingCachedServletRequest;
        }
    }

    /**
     * Use a specialized exception to abort the control flow in the test to ensure that no other,
     * accidentially thrown exception is masked.
     */
    private static class AbortControlFlowException extends RuntimeException
    {
        public AbortControlFlowException()
        {
            super();
        }
    }
}
