/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.RegistryContext;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.registry.RegistryFacade;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import org.apache.commons.beanutils.Converter;

public abstract class AbstractConverterTestCase extends AbstractMuleTestCase
{

    public void testNullConverter()
    {
        try
        {
            getConverter().convert(getValidConvertedType().getClass(), null);
            fail("Should thow exception on null");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testObjectAlreadyConverted()
    {
        Object obj = getValidConvertedType();
        Object result = getConverter().convert(obj.getClass(), obj);

        assertNotNull(result);
        assertEquals(obj, result);
    }

    public void testValidConversion()
    {
        Mock mockRegistry = new Mock(RegistryFacade.class);
        RegistryContext.setRegistry((RegistryFacade)mockRegistry.proxy());
        Object obj = getValidConvertedType();
        mockRegistry.expectAndReturn(getLookupMethod(), C.eq("test://Test"), obj);
        Object result = getConverter().convert(obj.getClass(), "test://Test");

        assertNotNull(result);
        mockRegistry.verify();
    }

    public void testInvalidConversion()
    {
        Mock mockRegistry = new Mock(RegistryFacade.class);
        RegistryContext.setRegistry((RegistryFacade)mockRegistry.proxy());
        Object obj = getValidConvertedType();
        mockRegistry.expectAndReturn(getLookupMethod(), C.eq("TestBad"), null);
        try
        {
            getConverter().convert(obj.getClass(), "TestBad");
            fail("should throw an exception if not found");
        }
        catch (Exception e)
        {
            // exprected
            mockRegistry.verify();
        }

    }

    public abstract Converter getConverter();

    public abstract Object getValidConvertedType();

    public abstract String getLookupMethod();
}
