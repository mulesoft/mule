/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.tck.testmodels.fruit.Apple;

/**
 * TODO
 */

public interface AutoTransformServiceInterface
{

    public Object doSomething(Apple data) throws Exception;
}
