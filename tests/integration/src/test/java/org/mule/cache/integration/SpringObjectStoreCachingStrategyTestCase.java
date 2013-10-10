/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cache.integration;

public class SpringObjectStoreCachingStrategyTestCase extends AbstractCachingStrategyTestCase
{

    @Override
    protected String getCachingStrategyConfigResource()
    {
        return "org/mule/cache/integration/spring-ehcache-caching-strategy-config.xml";
    }
}
