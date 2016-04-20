/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.Serializable;

@Alias("harvest-apples")
public class HarvestApplesSource extends Source<Apple, Serializable>
{

    @UseConfig
    AppleConfig appleConfig;

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {

    }
}
