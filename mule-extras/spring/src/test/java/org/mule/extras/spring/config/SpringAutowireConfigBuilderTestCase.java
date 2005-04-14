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
package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.umo.manager.UMOManager;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SpringAutowireConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{

    public String getConfigResource()
    {
        return "test-mule-autowire-app-context.xml,test-application-context.xml";
    }

    public ConfigurationBuilder getConfigBuilder()
    {
        return new SpringConfigurationBuilder();
    }

    public void testComponentResolverConfig() throws Exception
        {
            //test container init
            UMOManager manager = MuleManager.getInstance();
            assertNotNull(manager.getContainerContext());

            Object object = manager.getContainerContext().getComponent("org.mule.tck.testmodels.fruit.FruitBowl");
            assertNotNull(object);
            assertTrue(object instanceof FruitBowl);
            FruitBowl bowl = (FruitBowl) object;
            assertTrue(bowl.hasBanana());
            assertTrue(bowl.hasApple());
        }

}
