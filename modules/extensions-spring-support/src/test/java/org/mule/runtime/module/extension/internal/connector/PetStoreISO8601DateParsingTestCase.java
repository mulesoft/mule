/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class PetStoreISO8601DateParsingTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {PetStoreConnector.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "petstore-iso8601-config.xml";
    }


    @Test
    public void testDateTimeWithTimeZone() throws Exception
    {
        LocalDateTime date = getDate("getWithTimeZone");
        assertDate(date);
        assertTime(date);
    }

    @Test
    public void testDateTime() throws Exception
    {
        LocalDateTime date = getDate("getWithDateTime");
        assertDate(date);
        assertTime(date);
    }

    @Test
    public void testDate() throws Exception
    {
        LocalDateTime date = getDate("getWithDate");
        assertDate(date);
    }


    public LocalDateTime getDate(String flowName) throws Exception
    {
        PetStoreClient client = flowRunner(flowName)
                .run().getMessage().getPayload();

        return client.getOpeningDate(); // 2008-09-15T15:53:00+05:00
    }

    private void assertDate(LocalDateTime openingDate)
    {
        String formattedDate = openingDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        assertTrue(formattedDate.length() > 0);
        assertEquals(openingDate.getYear(), 2008);
        assertEquals(openingDate.getMonthValue(), 9);
        assertEquals(openingDate.getDayOfMonth(), 15);
    }

    private void assertTime(LocalDateTime openingDate)
    {
        String formattedDate = openingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertTrue(formattedDate.length() > 0);
    }
}
