/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ConfigLevelOperationTestCase extends ExtensionFunctionalTestCase
{

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {Apple.class}, {Banana.class}, {Kiwi.class}
        });
    }

    private final Class<? extends Fruit> fruitType;

    public ConfigLevelOperationTestCase(Class<? extends Fruit> fruitType)
    {
        this.fruitType = fruitType;
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class[] {VeganExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "vegan-config.xml";
    }

    @Test
    public void execute() throws Exception
    {
        Fruit fruit = (Fruit) flowRunner(fruitType.getSimpleName().toLowerCase() + "Ok").run().getMessage().getPayload();
        assertThat(fruit.getClass(), equalTo(fruitType));
        assertThat(fruit.isBitten(), is(true));
    }
}
