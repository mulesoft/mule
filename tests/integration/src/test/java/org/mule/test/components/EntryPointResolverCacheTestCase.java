/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.components;

import org.mule.api.MuleMessage;
import org.mule.message.DefaultExceptionPayload;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test an entry-point resolver used for multiple classes
 */
public class EntryPointResolverCacheTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/components/entry-point-resolver-cache-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/components/entry-point-resolver-cache-flow.xml"}});
    }

    public EntryPointResolverCacheTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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

        assertTrue("should be a string", payload instanceof String);
        assertEquals("ServiceOne", payload);

        response = clt.send("refTwoInbound", "another request", propertyMap);
        payload = response.getPayload();
        if ((payload == null) || (response.getExceptionPayload() != null))
        {
            DefaultExceptionPayload exPld = (DefaultExceptionPayload) response.getExceptionPayload();
            if (exPld.getException() != null)
            {
                fail(exPld.getException().getMessage());
            }
            else
            {
                fail(exPld.toString());
            }
        }
        assertTrue("should be a string", payload instanceof String);
        assertEquals("ServiceTwo", payload);

    }

    public interface ReferenceDataService
    {
        String retrieveReferenceData(String refKey);
    }

    public static class RefDataServiceOne implements ReferenceDataService
    {
        @Override
        public String retrieveReferenceData(String refKey)
        {
            return "ServiceOne";
        }
    }

    public static class RefDataServiceTwo implements ReferenceDataService
    {
        @Override
        public String retrieveReferenceData(String refKey)
        {
            return "ServiceTwo";
        }
    }
}
