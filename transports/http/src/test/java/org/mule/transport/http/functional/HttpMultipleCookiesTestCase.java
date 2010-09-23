/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.tck.FunctionalTestCase;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;

public class HttpMultipleCookiesTestCase extends FunctionalTestCase
{
    private static final int LOCAL_JETTY_SERVER_PORT = 4020;
    protected static String TEST_MESSAGE = "Test Http Request ";
    protected static final Log logger = LogFactory.getLog(HttpMultipleCookiesTestCase.class);

    private CountDownLatch simpleServerLatch = new CountDownLatch(1);
    private static AtomicBoolean cookiesRecieved = new AtomicBoolean(false);

    @Override
    protected void suitePreSetUp() throws Exception
    {
        super.suitePreSetUp();
        startServer();
        assertTrue(simpleServerLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Override
    protected String getConfigResources()
    {
        return "http-multiple-cookies-test.xml";
    }

    public void testSendDirectly() throws Exception
    {
        sendMessage(LOCAL_JETTY_SERVER_PORT);
    }

    public void testSendviaMule() throws Exception
    {
        sendMessage(4019);
    }

    protected void sendMessage(int port) throws Exception
    {
        HttpClient client2 = new HttpClient();
        client2.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        HttpState state = new HttpState();
        Cookie cookie1 = new Cookie("localhost", "TheFirst", "First", "/", null, false);
        state.addCookie(cookie1);
        Cookie cookie2 = new Cookie("localhost", "TheSecond", "Value2", "/", null, false);
        state.addCookie(cookie2);
        Cookie cookie3 = new Cookie("localhost", "TheThird", "Value3", "/", null, false);
        state.addCookie(cookie3);

        client2.setState(state);
        PostMethod method = new PostMethod("http://localhost:" + port);
        client2.executeMethod(method);
        assertEquals(TEST_MESSAGE, method.getResponseBodyAsString());
        assertTrue("Cookies were not recieved", cookiesRecieved.get());

        for (Cookie cookie : client2.getState().getCookies())
        {
            logger.debug(cookie.getName() + " " + cookie.getValue());
        }
        assertEquals(6, client2.getState().getCookies().length);

    }

    protected void startServer() throws Exception
    {
        logger.debug("server starting");
        Server server = new Server();
        Connector connector = new SocketConnector();
        connector.setPort(LOCAL_JETTY_SERVER_PORT);
        server.setConnectors(new Connector[]{connector});

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(HelloServlet.class.getName(), "/");

        server.start();
        // server.join();
        simpleServerLatch.countDown();
        logger.debug("Server started");

    }

    public static class HelloServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
        {
            try
            {
                response.setContentType("text/xml");
                response.setContentLength(TEST_MESSAGE.length());
                for (int i = 0; i < 3; i++)
                {
                    javax.servlet.http.Cookie cookie1 = new javax.servlet.http.Cookie("OutputCookieName" + i,
                        "OutputCookieValue" + i);
                    response.addCookie(cookie1);
                }
                cookiesRecieved.set(false);
                javax.servlet.http.Cookie[] cookies = request.getCookies();
                if (cookies != null)
                {
                    for (javax.servlet.http.Cookie cookie : cookies)
                    {
                        logger.debug(cookie.getName() + " " + cookie.getValue());
                        cookiesRecieved.set(true);
                    }
                }
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(TEST_MESSAGE);

            }
            catch (Exception e)
            {
                logger.error("Servlet error", e);
                throw new ServletException(e);
            }
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
        {
            doGet(request, response);
        }

    }
}
