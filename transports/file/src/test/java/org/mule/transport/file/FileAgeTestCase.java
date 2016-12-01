/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static java.lang.Long.parseLong;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;


public class FileAgeTestCase extends FunctionalTestCase
{

    private static final String CONNECTOR_FILE_AGE= "60000";
    private static final String ENDPOINT_FILE_AGE = "120000";

    @Rule
    public SystemProperty fileAgeConnectorSystemProperty = new SystemProperty("fileAgeConnector", CONNECTOR_FILE_AGE);

    @Rule
    public SystemProperty fileAgeEndpointSystemProperty = new SystemProperty("fileAgeEndpoint", ENDPOINT_FILE_AGE);

    @Override
    protected String getConfigFile()
    {
        return "file-age-config.xml";
    }

    @Test
    public void testFileAgeConnectorValue()
    {
        FileConnector fileConnector = muleContext.getRegistry().lookupObject("fileConnector");
        assertThat(fileConnector.getFileAge(), is(parseLong(CONNECTOR_FILE_AGE)));
    }

}
