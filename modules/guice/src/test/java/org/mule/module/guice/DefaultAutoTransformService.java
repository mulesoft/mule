/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.BananaFactory;

import com.google.inject.Inject;

/**
 * A test service
 */
public class DefaultAutoTransformService implements AutoTransformServiceInterface
{
    //An Orange is sent but we recieve an Apple through auto transformation
    public Object doSomething(Apple data) throws Exception
    {
        return data;
    }
}
