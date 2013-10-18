/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.mule.transformer.types.MimeTypes;

import org.ibeans.annotation.IntegrationBean;
import org.ibeans.api.CallException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ErrorFiltersTestCase extends AbstractIBeansTestCase
{
    @IntegrationBean
    private ErrorFilterIBean errorFilter;

    @Test
    public void testJsonFilter() throws Exception
    {
        //No error here
        errorFilter.jsonErrorFilter("{\"message\" : \"hello\"}", MimeTypes.JSON);

        try
        {
            errorFilter.jsonErrorFilter("{\"message\" : {\"error\" : 1234, \"errorMsg\" : \"it didnt work dude\"}}", MimeTypes.JSON);
            fail("Error should have been caught");
        }
        catch (CallException e)
        {
            //expected
            assertEquals("1234", e.getErrorCode());
            assertTrue(e.getMessage().contains("it didnt work dude"));
        }

        try
        {
            errorFilter.jsonErrorFilter("{\"message\" : {\"errorMsg\" : \"it didnt work dude\"}}", MimeTypes.JSON);
            fail("Error should have been caught");
        }
        catch (CallException e)
        {
            //expected
            assertTrue(e.getMessage().contains("it didnt work dude"));
        }
    }

    @Test
    public void testXmlFilter() throws Exception
    {
        //No error here
        errorFilter.jsonErrorFilter("<message>hello</message>", MimeTypes.XML);

        try
        {
            errorFilter.jsonErrorFilter("<message><error>1234</error><errorMsg>it didnt work dude</errorMsg></message>", MimeTypes.XML);
            fail("Error should have been caught");
        }
        catch (CallException e)
        {
            //expected
            assertEquals("1234", e.getErrorCode());
            assertTrue(e.getMessage().contains("it didnt work dude"));
        }

        try
        {
            errorFilter.jsonErrorFilter("<message><errorMsg>it didnt work dude</errorMsg></message>", MimeTypes.XML);
            fail("Error should have been caught");
        }
        catch (CallException e)
        {
            //expected
            assertTrue(e.getMessage().contains("it didnt work dude"));
        }
    }

    @Test
    public void testRegexFilter() throws Exception
    {
        //No error here
        errorFilter.jsonErrorFilter("<message>hello</message>", MimeTypes.TEXT);

        try
        {
            errorFilter.jsonErrorFilter("<message><error>1234</error><errorMsg>it didnt work dude</errorMsg></message>", MimeTypes.TEXT);
            fail("Error should have been caught");
        }
        catch (CallException e)
        {
            //expected
            assertTrue(e.getMessage().contains("it didnt work dude"));
        }

        try
        {
            errorFilter.jsonErrorFilter("<message><errorMsg>it didnt work dude</errorMsg></message>", MimeTypes.TEXT);
            fail("Error should have been caught");
        }
        catch (CallException e)
        {
            //expected
            assertTrue(e.getMessage().contains("it didnt work dude"));
        }
    }

}
