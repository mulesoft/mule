/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.builders;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MuleXmlBuilderContextServlet extends HttpServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2446689032349402434L;

    private MuleXmlBuilderContextListener contextListener;

    @Override
    public void init() throws ServletException
    {
        if (contextListener != null)
        {
            contextListener.destroy();
            contextListener = null;
        }
        contextListener = new MuleXmlBuilderContextListener();
        contextListener.initialize(getServletContext());
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        getServletContext().log(
            "("
            + request.getRequestURI()
            + ")"
            + "MuleXmlBuilderContextServlet.service(HttpServletRequest request, HttpServletResponse response) call ignored.");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Override
    public void destroy()
    {
        if (contextListener != null)
        {
            contextListener.destroy();
        }
    }
}
