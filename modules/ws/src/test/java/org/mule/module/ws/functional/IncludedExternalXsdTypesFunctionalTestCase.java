/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import static org.mule.util.ClassUtils.getClassPathRoot;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Rule;

/**
 * Uses a WSDL definition file that imports the types from another WSDL file, which includes the schema from yet another
 * XSD file but via HTTP.
 */
public class IncludedExternalXsdTypesFunctionalTestCase extends IncludedXsdTypesFunctionalTestCase
{
    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");
    @Rule
    public SystemProperty wsdlLocation = new SystemProperty("wsdlLocation", "TestExternalIncludedTypes.wsdl");

    private Server server;
    File wsdl;

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        createWsdlFile();
        startServer();
    }

    private void createWsdlFile() throws IOException
    {
        //the WSDL must reference a dynamic HTTP port so we have to create it
        String modifiedWsdl = String.format(IOUtils.getResourceAsString("TestIncludedExternalTypeDefinitionsFormat.wsdl", this.getClass()), httpPort.getValue());
        String testRoot = getClassPathRoot(IncludedExternalXsdTypesFunctionalTestCase.class).getPath();
        wsdl = new File(testRoot + "TestIncludedExternalTypeDefinitions.wsdl");
        FileUtils.writeStringToFile(wsdl, modifiedWsdl);
    }

    private void startServer() throws Exception
    {
        server = new Server(httpPort.getNumber());
        server.setHandler(new SchemaProviderHandler());
        server.start();
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        server.stop();
        FileUtils.deleteQuietly(wsdl);
        super.doTearDownAfterMuleContextDispose();
    }

    private class SchemaProviderHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            response.setContentType("application/xml");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(IOUtils.getResourceAsString("TestSchema.xsd", this.getClass()));
            baseRequest.setHandled(true);
        }
    }
}
