/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mule.util.xmlsecurity.XMLSecureFactories.EXTERNAL_ENTITIES_PROPERTY;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.util.concurrent.Latch;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.mule.util.IOUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class SchemaValidationTestCase extends AbstractMuleTestCase
{

    // this is disabled (secure) by default, so we need to change it for the test
    @Rule
    public SystemProperty externalEntities = new SystemProperty(EXTERNAL_ENTITIES_PROPERTY, "true");

    private static final String SIMPLE_SCHEMA = "schema/schema1.xsd";

    private static final String INCLUDE_SCHEMA = "schema/schema-with-include.xsd";

    private static final String VALID_XML_FILE = "/validation1.xml";

    private static final String INVALID_XML_FILE = "/validation2.xml";

    private final TestErrorHandler errorHandler = new TestErrorHandler();

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Test
    public void testValidate() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(SIMPLE_SCHEMA);
        filter.initialise();

        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(VALID_XML_FILE), muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(INVALID_XML_FILE), muleContext)), is(false));
    }

    @Test
    public void testDefaultResourceResolverIsPresent() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(SIMPLE_SCHEMA);
        filter.initialise();

        assertThat(filter.getResourceResolver(), is(not(nullValue())));
    }

    @Test
    public void testValidateWithIncludes() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(INCLUDE_SCHEMA);
        filter.initialise();

        MuleMessage message = new DefaultMuleMessage(getClass().getResourceAsStream(VALID_XML_FILE), muleContext);
        assertThat(filter.accept(message), is(true));
        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(INVALID_XML_FILE), muleContext)), is(false));
    }

    @Test
    public void testValidationDataTypeNotModified() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(SIMPLE_SCHEMA);
        filter.initialise();

        MuleMessage message = new DefaultMuleMessage(null, muleContext);
        message.setPayload(IOUtils.toString(getClass().getResourceAsStream(VALID_XML_FILE)), DataTypeFactory.XML_STRING);
        assertThat(filter.accept(message), is(true));
        assertThat(message.getDataType().getMimeType(), is("text/xml"));
        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(INVALID_XML_FILE), muleContext)), is(false));
    }

    @Test
    public void testErrorHandler() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaLocations(SIMPLE_SCHEMA);
        filter.initialise();
        filter.setErrorHandler(errorHandler);
        assertThat(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream(INVALID_XML_FILE), muleContext)), is(false));
        assertThat(errorHandler.exception, is(notNullValue()));
    }

    @Test
    public void testConcurrentSchemaValidationFilterCreation() throws InterruptedException
    {
        int threadsNum = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadsNum);

        final Latch executeLatch = new Latch();
        final CountDownLatch readyForExecutionLatch = new CountDownLatch(threadsNum);
        final List<String> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadsNum; i++)
        {
            executorService.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    SchemaValidationFilter filter = new SchemaValidationFilter();
                    filter.setSchemaLocations(INCLUDE_SCHEMA);
                    try
                    {
                        readyForExecutionLatch.countDown();
                        executeLatch.await();
                        filter.initialise();
                    }
                    catch (InitialisationException e)
                    {
                        errors.add(e.getDetailedMessage());
                    }
                    catch (InterruptedException e)
                    {
                        errors.add(e.getMessage());
                    }
                }
            });
        }

        if (!readyForExecutionLatch.await(5, SECONDS))
        {
            fail("Tasks aren't ready to run");
        }

        executeLatch.release();

        executorService.shutdown();

        if (!executorService.awaitTermination(5, SECONDS))
        {
            fail("Tasks didn't finish running");
        }

        assertEquals("Executions failed:\n" + errors, 0, errors.size());
    }

    private class TestErrorHandler implements ErrorHandler
    {

        private Exception exception = null;

        @Override
        public void warning(SAXParseException exception) throws SAXParseException
        {
            this.exception = exception;
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXParseException
        {
            this.exception = exception;
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXParseException
        {
            this.exception = exception;
            throw exception;
        }
    }

}
