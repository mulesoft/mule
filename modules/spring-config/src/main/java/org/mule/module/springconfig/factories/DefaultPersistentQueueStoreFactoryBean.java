/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.factories;

import org.mule.api.config.MuleProperties;

public class DefaultPersistentQueueStoreFactoryBean extends ObjectStoreFromRegistryFactoryBean
{
    public DefaultPersistentQueueStoreFactoryBean()
    {
        super(MuleProperties.QUEUE_STORE_DEFAULT_PERSISTENT_NAME);
    }
}
