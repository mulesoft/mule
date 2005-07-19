/* 

 * $Header$

 * $Revision$

 * $Date$

 * ------------------------------------------------------------------------------------------------------

 * 

 * Copyright (c) SymphonySoft Limited. All rights reserved.

 * http://www.symphonysoft.com

 * 

 * The software in this package is published under the terms of the BSD

 * style license a copy of which has been included with this distribution in

 * the LICENSE.txt file. 

 *

 */

package org.mule.providers.http.servlet;

import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOMessage;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class DefaultMuleHttpReceiverServlet extends AbstractReceiverServlet
{
    AbstractMessageReceiver receiver = null;

    protected void doInit(ServletConfig servletConfig) throws ServletException {
        receiver = (AbstractMessageReceiver)servletConfig.getServletContext().getAttribute("messageReceiver");
        if (receiver == null) {
            throw new ServletException(new Message("http", 7).toString());
        }
    }

    public void doInit() throws ServletException {
       receiver = (AbstractMessageReceiver) getServletContext().getAttribute("messageReceiver");
        if (receiver == null) {
            throw new ServletException(new Message("http", 7).toString());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {
            UMOMessage responseMessage = null;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            responseMessage = receiver.routeMessage(requestMessage, true);
            writeResponse(response, responseMessage);

        } catch (Exception e) {
            handleException(e, e.getMessage(), response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            UMOMessage responseMessage = null;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            responseMessage = receiver.routeMessage(requestMessage, receiver.getEndpoint().isSynchronous());
            if (responseMessage != null) {
                writeResponse(response, responseMessage);
            }
        } catch (Exception e) {
            handleException(e, e.getMessage(), response);
        }
    }
}