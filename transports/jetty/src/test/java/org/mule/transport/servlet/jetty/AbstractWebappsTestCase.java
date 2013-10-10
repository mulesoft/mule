/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;

import static org.junit.Assert.assertEquals;

public abstract class AbstractWebappsTestCase extends FunctionalTestCase
{
    @Override
    protected boolean isStartContext()
    {
        // prepare the test webapp before starting Mule
        return false;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        final URL url = ClassUtils.getClassPathRoot(getClass());
        File webapps = new File(url.getFile(), "../webapps");
        FileUtils.deleteDirectory(webapps);
        webapps.mkdir();

        FileUtils.copyFile(new File(url.getFile(), "../../src/test/resources/test.war"),
                           new File(webapps, "test.war"));

        muleContext.start();
    }

    protected void sendRequestAndAssertCorrectResponse(String url) throws IOException
    {
        GetMethod method = new GetMethod(url);
        int rc = new HttpClient().executeMethod(method);
        assertEquals(HttpStatus.SC_OK, rc);
        assertEquals("Hello", method.getResponseBodyAsString());
    }
}


