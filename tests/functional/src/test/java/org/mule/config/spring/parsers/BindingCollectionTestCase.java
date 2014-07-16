/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import static org.junit.Assert.assertEquals;

import org.mule.config.spring.parsers.beans.OrphanBean;

import java.util.Collection;

import org.junit.Test;

public class BindingCollectionTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/nested-collection-test.xml";
    }

    @Test
    public void testAll()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan1", OrphanBean.class);
        Collection<?> kids = (Collection<?>) assertContentExists(orphan.getKids(), Collection.class);
        assertEquals(5, kids.size());
    }
}
