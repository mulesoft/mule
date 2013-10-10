/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import org.mule.api.MuleMessage;
import org.mule.message.DefaultExceptionPayload;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test an entry-point resolver used for multiple classes
 */
public class EntryPointResolverCacheTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/components/entry-point-resolver-cache.xml";
    }

    @Test
    public void testCache() throws Exception
    {
        MuleClient clt = new MuleClient(muleContext);

        MuleMessage response = null;
        HashMap<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("method", "retrieveReferenceData");

        response = clt.send("refOneInbound", "a request", propertyMap);
        Object payload = response.getPayload();

        assertTrue("should be a string", payload instanceof String );
        assertEquals("ServiceOne", payload);

        response = clt.send("refTwoInbound", "another request", propertyMap);
        payload = response.getPayload();
        if((payload == null) || (response.getExceptionPayload() != null))
        {
            DefaultExceptionPayload exPld = (DefaultExceptionPayload)response.getExceptionPayload();
            if(exPld.getException() != null)
            {
                fail(exPld.getException().getMessage());
            }
            else
            {
                fail(exPld.toString());
            }
        }
        assertTrue("should be a string", payload instanceof String );
        assertEquals("ServiceTwo", payload);

    }

    public interface ReferenceDataService
    {
        String retrieveReferenceData(String refKey);
    }

    public static class RefDataServiceOne implements ReferenceDataService
    {
        public String retrieveReferenceData(String refKey)
        {
            return "ServiceOne";
        }
    }

    public static class RefDataServiceTwo implements ReferenceDataService
    {
        public String retrieveReferenceData(String refKey)
        {
            return "ServiceTwo";
        }

    }
}
