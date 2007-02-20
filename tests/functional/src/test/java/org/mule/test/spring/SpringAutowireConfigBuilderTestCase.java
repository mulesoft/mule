/*
 * $Id:SpringAutowireConfigBuilderTestCase.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.tck.testmodels.fruit.FruitBowl;

public class SpringAutowireConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{

    public String getConfigResources()
    {
        return "org/mule/test/spring/test-mule-autowire-app-context.xml," +
                "org/mule/test/spring/test-application-context.xml";
    }

    public ConfigurationBuilder getBuilder()
    {
        return new MuleXmlConfigurationBuilder();
    }

    public void testComponentResolverConfig() throws Exception
    {
        // test container init
        assertNotNull(managementContext.getRegistry().getContainerContext());

        Object object =managementContext.getRegistry().getContainerContext().getComponent(
            new ContainerKeyPair("spring", "org.mule.tck.testmodels.fruit.FruitBowl"));
        assertNotNull(object);
        assertTrue(object instanceof FruitBowl);
        FruitBowl bowl = (FruitBowl)object;
        assertTrue(bowl.hasBanana());
        assertTrue(bowl.hasApple());
    }

}
