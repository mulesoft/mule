/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleEvent;
import org.mule.common.Testable;
import org.mule.common.metadata.ConnectorMetaDataEnabled;
import org.mule.module.db.internal.domain.database.DbConfig;

/**
 * Resolves a {@link DbConfig} for a given {@link MuleEvent}
 */
public interface DbConfigResolver extends AnnotatedObject, Testable, ConnectorMetaDataEnabled
{

    /**
     * Resolves which database configuration to use for a given event
     *
     * @param muleEvent event used to resolve the configuration. Not null.
     * @return a non null database configuration to use to process the given event
     * @throws UnresolvableDbConfigException when is not possible to resolve a database configuration
     */
    DbConfig resolve(MuleEvent muleEvent) throws UnresolvableDbConfigException;
}
