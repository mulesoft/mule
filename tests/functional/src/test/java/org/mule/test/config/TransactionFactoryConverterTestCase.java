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
import org.mule.config.converters.TransactionFactoryConverter;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.umo.UMOTransactionFactory;

public class TransactionFactoryConverterTestCase extends AbstractConverterTestCase
{

    public Converter getConverter()
    {
        return new TransactionFactoryConverter();
    }

    public Object getValidConvertedType()
    {
        return new TestTransactionFactory();
    }

    public String getLookupMethod()
    {
        return null;
    }

    public void testValidConversion()
    {
        Object obj = getConverter().convert(UMOTransactionFactory.class,
            TestTransactionFactory.class.getName());
        assertNotNull(obj);
        assertTrue(obj instanceof TestTransactionFactory);
    }

    public void testInvalidConversion()
    {
        try
        {
            getConverter().convert(UMOTransactionFactory.class, "foo.bar.bad.TransactionFactory");
            fail("should throw exception on bad transaction factory class");
        }
        catch (Exception e)
        {
            // expected
        }

    }

}
