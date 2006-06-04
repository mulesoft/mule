/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.config;

import org.apache.commons.beanutils.Converter;
import org.mule.config.converters.EndpointConverter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class EndpointConverterTestCase extends AbstractMuleTestCase
{
    public Converter getConverter()
    {
        return new EndpointConverter();
    }

    public Object getValidConvertedType()
    {
        try {
            return MuleTestUtils.getTestEndpoint("test://Test", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    public String getLookupMethod()
    {
        return "lookupEndpointIdentifier";
    }

    public void testValidConversion()
    {

        // MuleManager.setInstance((UMOManager)mockManager.proxy());
        // mockManager.expectAndReturn("lookupEndpointIdentifier",
        // C.args(C.isA(String.class), C.isA(String.class)),
        // "test://test");
        // mockManager.expectAndReturn("lookupEndpointIdentifier",
        // C.args(C.isA(String.class), C.isA(String.class)),
        // "test://test");
        // mockManager.expectAndReturn("lookupEndpointIdentifier",
        // C.args(C.isA(String.class), C.isA(String.class)),
        // "test://test");
        // mockManager.expectAndReturn("lookupEndpoint", "test://test", null);
        // mockManager.expectAndReturn("lookupEndpoint", "test://test", null);
        //
        // Object obj = getValidConvertedType();
        // mockManager.expectAndReturn("lookupEndpointIdentifier",
        // C.args(C.isA(String.class), C.isA(String.class)),
        // "test://test");
        //
        // Object result = getConverter().convert(obj.getClass(),
        // "test://Test");
        //
        // assertNotNull(result);
        // mockManager.verify();
    }

    public void testInvalidConversion()
    {
        // MuleManager.setInstance((UMOManager)mockManager.proxy());
        // Object obj = getValidConvertedType();
        // mockManager.expectAndReturn("lookupEndpointIdentifier",
        // C.args(C.isA(String.class), C.isA(String.class)),
        // null);
        //
        // try
        // {
        // getConverter().convert(obj.getClass(), "TestBad");
        // fail("should throw an exception if not valid");
        // } catch (Exception e)
        // {
        // //exprected
        // mockManager.verify();
        // }
    }
}
