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

import org.apache.commons.beanutils.Converter;
import org.mule.MuleManager;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.manager.UMOManager;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

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
        Mock mockManager = new Mock(UMOManager.class);
        try
        {
            MuleManager.setInstance((UMOManager)mockManager.proxy());
            Object obj = getValidConvertedType();
            mockManager.expectAndReturn(getLookupMethod(), C.eq("test://Test"), obj);
            Object result = getConverter().convert(obj.getClass(), "test://Test");

            assertNotNull(result);
            mockManager.verify();
        }
        finally
        {
            MuleManager.setInstance(null);
        }
    }

    public void testInvalidConversion()
    {
        Mock mockManager = new Mock(UMOManager.class);
        MuleManager.setInstance((UMOManager)mockManager.proxy());
        Object obj = getValidConvertedType();
        mockManager.expectAndReturn(getLookupMethod(), C.eq("TestBad"), null);
        try
        {
            getConverter().convert(obj.getClass(), "TestBad");
            fail("should throw an exception if not found");
        }
        catch (Exception e)
        {
            // exprected
            mockManager.verify();
        }
        finally
        {
            MuleManager.setInstance(null);
        }

    }

    public abstract Converter getConverter();

    public abstract Object getValidConvertedType();

    public abstract String getLookupMethod();
}
