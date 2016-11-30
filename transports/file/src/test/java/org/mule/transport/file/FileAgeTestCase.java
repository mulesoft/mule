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
    @Rule
    public SystemProperty fileAgeConnectorSystemProperty = new SystemProperty("fileAgeConnector","60000");

    @Rule
    public SystemProperty fileAgeEndpointSystemProperty = new SystemProperty("fileAgeEndpoint","120000");

    @Override
    protected String getConfigFile()
    {
        return "file-age-config.xml";
    }

    @Test
    public void testFileAgeConnectorValue()
    {
        FileConnector fileConnector = (FileConnector) muleContext.getRegistry().lookupConnector("fileConnector");
        assertThat(fileConnector.getFileAge(),is(parseLong(fileAgeConnectorSystemProperty.getValue())));

    }

}
