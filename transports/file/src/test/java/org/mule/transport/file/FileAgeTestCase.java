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
import static org.mule.transport.file.FileTestUtils.createDataFile;
import static org.mule.util.FileUtils.openDirectory;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class FileAgeTestCase extends FunctionalTestCase
{

    private static long DELTA_TIME = 1000;

    private static final String CONNECTOR_FILE_AGE= "2000";

    private static final String ENDPOINT_FILE_AGE = "3000";

    @Rule
    public SystemProperty fileAgeConnectorSystemProperty = new SystemProperty("fileAgeConnector", CONNECTOR_FILE_AGE);

    @Rule
    public SystemProperty fileAgeEndpointSystemProperty = new SystemProperty("fileAgeEndpoint", ENDPOINT_FILE_AGE);

    private File temporaryDirectory ;

    private File secondTemporaryDirectory ;

    private File thirdTemporaryDirectory ;

    @Override
    protected String getConfigFile()
    {
        return "file-age-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        openDirectory(getFileInsideWorkingDirectory("test").getAbsolutePath());
        temporaryDirectory = openDirectory(getFileInsideWorkingDirectory("test").getAbsolutePath());
        secondTemporaryDirectory = openDirectory(getFileInsideWorkingDirectory("test2").getAbsolutePath());
        thirdTemporaryDirectory = openDirectory(getFileInsideWorkingDirectory("test3").getAbsolutePath());
    }

    @Test
    public void testFileAgeConnectorValue()
    {
        FileConnector fileConnector = muleContext.getRegistry().lookupObject("fileConnector");
        assertThat(fileConnector.getFileAge(), is(parseLong(CONNECTOR_FILE_AGE)));
    }

    @Test
    public void fileAgeInheritedFromConnectorIsHonored() throws Exception
    {
        File createdFile = createDataFile(temporaryDirectory, TEST_MESSAGE, "UTF-8");
        Thread.sleep(parseLong(CONNECTOR_FILE_AGE) - DELTA_TIME);
        assertThat(createdFile.exists(), is(true));
    }

    @Test
    public void overrodeFileAgeInEndpointIsHonored() throws Exception
    {
        File createdFile = createDataFile(secondTemporaryDirectory, TEST_MESSAGE, "UTF-8");
        Thread.sleep(parseLong(ENDPOINT_FILE_AGE) - DELTA_TIME);
        assertThat(createdFile.exists(), is(true));
    }

    @Test
    public void fileAgeInEndpointIsHonoredWhenAbsentInReferredConnector() throws Exception
    {
        File createdFile = createDataFile(thirdTemporaryDirectory, TEST_MESSAGE, "UTF-8");
        Thread.sleep(parseLong(ENDPOINT_FILE_AGE) - DELTA_TIME);
        assertThat(createdFile.exists(), is(true));
    }

}
