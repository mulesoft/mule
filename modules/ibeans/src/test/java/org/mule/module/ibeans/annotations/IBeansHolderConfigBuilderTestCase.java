/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.mule.module.ibeans.config.IBeanHolder;
import org.mule.module.ibeans.config.IBeansLoader;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IBeansHolderConfigBuilderTestCase extends AbstractIBeansTestCase
{
    @Override
    protected void doTearDown() throws Exception
    {
        System.getProperties().remove(IBeansLoader.SCAN_PACKAGES_PROPERTY);
    }

    @Test
    public void testConfigBuilder() throws Exception
    {
        Collection<IBeanHolder> col = muleContext.getRegistry().lookupObjects(IBeanHolder.class);
        //Ensure IBeanHolder is comparable
        Set<IBeanHolder> beans = new TreeSet<IBeanHolder>(col);

        int exprectedSize = 11;
        assertEquals(exprectedSize, beans.size());
        String[] ids = new String[exprectedSize];
        int i = 0;
        for (Iterator<IBeanHolder> iterator = beans.iterator(); iterator.hasNext(); i++)
        {
            IBeanHolder iBeanHolder = iterator.next();
            ids[i] = iBeanHolder.getId();
        }

        assertEquals("errorfilter", ids[0]);
        //these are loaded from  a jar on the classpath, part test for MULE-5108
        assertEquals("flickr", ids[1]);
        assertEquals("flickrauthentication", ids[2]);
        assertEquals("flickrsearch", ids[3]);
        assertEquals("flickrupload", ids[4]);
        assertEquals("hostip", ids[5]);
        assertEquals("search", ids[6]);
        assertEquals("testexception", ids[7]);
        assertEquals("testimplicitpropertiesinfactory", ids[8]);
        assertEquals("testparamsfactory", ids[9]);
        assertEquals("testuri", ids[10]);
    }
}
