/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.util.FileUtils.deleteFile;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.UUID;

import java.io.File;

import net.sf.saxon.Controller;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XsltResultDocumentTestCase extends FunctionalTestCase
{

    private static final String INPUT_FILE = "cities.xml";
    private static final String OUTPUT_FILE_PROPERTY = "outputFile";
    private static final String FLOW_NAME_WITH_CONTROLLER_RESET = "listCitiesWithControllerReset";
    private static final String FLOW_NAME_WITHOUT_CONTROLLER_RESET = "listCitiesWithoutControllerReset";
    private static final String FLOW_NAME_SIMPLE_LIST_CITIES = "listCities";
    private static final String EXPECTED_OUTPUT = "italy - milan - 5 | france - paris - 7 | germany - munich - 4 | france - lyon - 2 | italy - venice - 1 | ";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    protected String getConfigFile()
    {
        return "xsl/xslt-result-document-config.xml";
    }

    @Test
    public void writeToSameFileSeveralTimes() throws Exception
    {
        String cities = IOUtils.getResourceAsString(INPUT_FILE, getClass());

        File outputFile = temporaryFolder.newFile(UUID.getUUID());

        executeFlowAndValidateOutput(cities, outputFile, FLOW_NAME_SIMPLE_LIST_CITIES);
        executeFlowAndValidateOutput(cities, outputFile, FLOW_NAME_SIMPLE_LIST_CITIES);
        Controller controller = ((net.sf.saxon.jaxp.TransformerImpl) TestTransformerFactoryImpl.TRANSFORMER).getUnderlyingController();
    }
    
    @Test
    public void writeToFileWithoutControllerReset() throws Exception
    {
        String cities = IOUtils.getResourceAsString(INPUT_FILE, getClass());

        File outputFile = temporaryFolder.newFile(UUID.getUUID());

        executeFlowAndValidateOutput(cities, outputFile, FLOW_NAME_WITHOUT_CONTROLLER_RESET);
        Controller controller = ((net.sf.saxon.jaxp.TransformerImpl) TestTransformerFactoryImpl.TRANSFORMER).getUnderlyingController();
        assertThat(controller.getInitialContextItem(), is(not(nullValue())));
    }
    
    @Test
    public void writeToFileWithControllerReset() throws Exception
    {
        String cities = IOUtils.getResourceAsString(INPUT_FILE, getClass());

        File outputFile = temporaryFolder.newFile(UUID.getUUID());

        executeFlowAndValidateOutput(cities, outputFile, FLOW_NAME_WITH_CONTROLLER_RESET);
        Controller controller = ((net.sf.saxon.jaxp.TransformerImpl) TestTransformerFactoryImpl.TRANSFORMER).getUnderlyingController();
        assertThat(controller.getInitialContextItem(), is(nullValue()));
    }


    private void executeFlowAndValidateOutput(String payload, File outputFile, String flow) throws Exception
    {
        deleteFile(outputFile);
        runFlow(flow, createEventWithPayloadAndSessionProperty(payload, OUTPUT_FILE_PROPERTY, outputFile.getAbsolutePath()));
        assertThat(FileUtils.readFileToString(outputFile), is(EXPECTED_OUTPUT));
    }

    private MuleEvent createEventWithPayloadAndSessionProperty(Object payload, String propertyName, Object propertyValue) throws Exception
    {
        MuleEvent muleEvent = getTestEvent(payload);
        muleEvent.getMessage().setProperty(propertyName, propertyValue, PropertyScope.SESSION);
        return muleEvent;
    }

}
