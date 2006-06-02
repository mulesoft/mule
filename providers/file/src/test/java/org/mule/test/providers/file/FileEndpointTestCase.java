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
 */
package org.mule.test.providers.file;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 * @version $Revision$
 */

public class FileEndpointTestCase extends AbstractMuleTestCase
{
    public void testFileUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("file:///C:/temp?endpointName=fileEndpoint");
        assertEquals("file", url.getScheme());
        assertEquals("/C:/temp", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("fileEndpoint", url.getEndpointName());
        assertEquals(-1, url.getPort());
        assertEquals("file:///C:/temp?endpointName=fileEndpoint", url.toString());
        assertEquals("endpointName=fileEndpoint", url.getQuery());
        assertEquals(1, url.getParams().size());
    }

    public void testFileUrlWithoutDrive() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("file://temp?endpointName=fileEndpoint");
        assertEquals("file", url.getScheme());
        assertEquals("temp", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("fileEndpoint", url.getEndpointName());
        assertEquals(-1, url.getPort());
        assertEquals("file://temp?endpointName=fileEndpoint", url.toString());
        assertEquals("endpointName=fileEndpoint", url.getQuery());
        assertEquals(1, url.getParams().size());
    }

    public void testRelativeFileUriParentDir() throws Exception
    {
        String muleURI = "file://../test-data/in";
        UMOEndpointURI url = new MuleEndpointURI(muleURI);

        assertEquals("../test-data/in", url.getAddress());
    }

    public void testRelativeFileUriCurrentDir() throws Exception
    {
        String muleURI = "file://./test-data/in";
        UMOEndpointURI url = new MuleEndpointURI(muleURI);

        assertEquals("./test-data/in", url.getAddress());
    }

    public void testWinNetworkUri() throws Exception
    {
        String muleURI = "file:////192.168.0.1/test/";
        UMOEndpointURI url = new MuleEndpointURI(muleURI);

        assertEquals("//192.168.0.1/test/", url.getAddress());
    }

    public void testRelativeFileUriAsParameter() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("file://?address=./temp&endpointName=fileEndpoint");
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
