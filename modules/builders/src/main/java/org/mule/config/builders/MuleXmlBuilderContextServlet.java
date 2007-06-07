/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    public void destroy()
    {
        if (contextListener != null)
        {
            contextListener.destroy();
        }
    }
}
