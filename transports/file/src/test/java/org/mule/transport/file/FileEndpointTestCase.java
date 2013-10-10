/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FileEndpointTestCase extends AbstractMuleContextTestCase
{

    private EndpointURI newMuleEndpointURI(String text) throws Exception
    {
        MuleEndpointURI uri = new MuleEndpointURI(text, muleContext);
        uri.initialise();
        return uri;
    }

    @Test
    public void testFileUrl() throws Exception
    {
        EndpointURI url = newMuleEndpointURI("file:///C:/temp?endpointName=fileEndpoint");
        assertEquals("file", url.getScheme());
        assertEquals("/C:/temp", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("fileEndpoint", url.getEndpointName());
        assertEquals(-1, url.getPort());
        assertEquals("file:///C:/temp?endpointName=fileEndpoint", url.toString());
        assertEquals("endpointName=fileEndpoint", url.getQuery());
        assertEquals(1, url.getParams().size());
    }

    @Test
    public void testFileUrlWithoutDrive() throws Exception
    {
        EndpointURI url = newMuleEndpointURI("file://temp?endpointName=fileEndpoint");
        assertEquals("file", url.getScheme());
        assertEquals("temp", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("fileEndpoint", url.getEndpointName());
        assertEquals(-1, url.getPort());
        assertEquals("file://temp?endpointName=fileEndpoint", url.toString());
        assertEquals("endpointName=fileEndpoint", url.getQuery());
        assertEquals(1, url.getParams().size());
    }

    @Test
    public void testRelativeFileUriParentDir() throws Exception
    {
        String muleURI = "file://../test-data/in";
        EndpointURI url = newMuleEndpointURI(muleURI);

        assertEquals("../test-data/in", url.getAddress());
    }

    @Test
    public void testRelativeFileUriCurrentDir() throws Exception
    {
        String muleURI = "file://./test-data/in";
        EndpointURI url = newMuleEndpointURI(muleURI);

        assertEquals("./test-data/in", url.getAddress());
    }

    @Test
    public void testWinNetworkUri() throws Exception
    {
        String muleURI = "file:////192.168.0.1/test/";
        EndpointURI url = newMuleEndpointURI(muleURI);

        assertEquals("//192.168.0.1/test/", url.getAddress());
    }

    @Test
    public void testRelativeFileUriAsParameter() throws Exception
    {
        EndpointURI url = newMuleEndpointURI("file://?address=./temp&endpointName=fileEndpoint");
        assertEquals("file", url.getScheme());
        assertEquals("./temp", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("fileEndpoint", url.getEndpointName());
        assertEquals(-1, url.getPort());
        assertEquals("file://?address=./temp&endpointName=fileEndpoint", url.toString());
        assertEquals("address=./temp&endpointName=fileEndpoint", url.getQuery());
        assertEquals(2, url.getParams().size());
    }

}
