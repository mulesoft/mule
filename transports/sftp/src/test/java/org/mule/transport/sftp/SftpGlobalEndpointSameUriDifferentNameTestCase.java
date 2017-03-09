/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.endpoint.EndpointURIEndpointBuilder;

import org.junit.Test;

public class SftpGlobalEndpointSameUriDifferentNameTestCase extends AbstractSftpFunctionalTestCase
{


    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-two-same-endpoints-different-name.xml";
    }

    @Test
    public void whenTwoSftpEndpointsWithSameConfigAndDifferentNameThenAppOk() throws Exception
    {
        EndpointURIEndpointBuilder endpointBuilder1 = muleContext.getRegistry().lookupObject("SFTP1");
        EndpointURIEndpointBuilder endpointBuilder2 = muleContext.getRegistry().lookupObject("SFTP2");
        assertThat(endpointBuilder1.getEndpointBuilder().getEndpoint().getUri().toString(), equalTo(endpointBuilder2.getEndpointBuilder().getEndpoint().getUri().toString()));
    }

}