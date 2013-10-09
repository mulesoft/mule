/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.OrphanBean;

import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BindingCollectionTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/nested-collection-test.xml";
    }

    @Test
    public void testAll()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan1", OrphanBean.class);
        Collection kids = (Collection) assertContentExists(orphan.getKids(), Collection.class);
        assertEquals(5, kids.size());
    }

}
