/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.domain.xa;

import org.mule.api.MuleContext;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;

import javax.sql.DataSource;

/**
 * Decorates {@link DataSource} if required in order to work with XA transactions
 */
public interface DataSourceDecorator
{

    /**
     * Decorates a dataSource
     *
     * @param dataSource dataSource to decorate. Non null
     * @param dataSourceName dataSource bean name
     * @param dbPoolingProfile pooling profile use to create the wrapped dataSource
     * @param muleContext mule context where the wrapped dataSource has to be registered
     * @return
     */
    DataSource decorate(DataSource dataSource, String dataSourceName, DbPoolingProfile dbPoolingProfile, MuleContext muleContext);

    /**
     * Indicates whether or not this decorator can decorate a given datasource
     *
     * @param dataSource dataSource to check
     * @param muleContext mule context associated to the current mule application
     * @return
     */
    boolean appliesTo(DataSource dataSource, MuleContext muleContext);

}
