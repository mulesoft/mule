/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
