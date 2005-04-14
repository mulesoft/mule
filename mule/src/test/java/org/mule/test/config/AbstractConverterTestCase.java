/*
 * $Header$
 * $Revision$
 * $Date$
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

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.apache.commons.beanutils.Converter;
import org.mule.MuleManager;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.manager.UMOManager;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractConverterTestCase extends AbstractMuleTestCase
{
    protected Mock mockManager = new Mock(UMOManager.class);

    public void testnullConverter()
    {
        try
        {
            getConverter().convert(getValidConvertedType().getClass(), null);
            fail("Should thow exception on null");
        } catch (Exception e)
        {
            //expected
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

        MuleManager.setInstance((UMOManager)mockManager.proxy());
        Object obj = getValidConvertedType();
        mockManager.expectAndReturn(getLookupMethod(), C.eq("test://Test"), obj);

        Object result = getConverter().convert(obj.getClass(), "test://Test");

        assertNotNull(result);
        mockManager.verify();
    }

    public void testInvalidConversion()
    {
        MuleManager.setInstance((UMOManager)mockManager.proxy());
        Object obj = getValidConvertedType();
        mockManager.expectAndReturn(getLookupMethod(), C.eq("TestBad"), null);
        try
        {
            getConverter().convert(obj.getClass(), "TestBad");
            fail("should throw an exception if not found");
        } catch (Exception e)
        {
            //exprected
            mockManager.verify();
        }

    }

    protected void tearDown() throws Exception
    {
        MuleManager.setInstance(null);
    }

    public abstract Converter getConverter();

    public abstract Object getValidConvertedType();

    public abstract String getLookupMethod();
}
