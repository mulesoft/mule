/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.xa;

import org.mule.api.MuleContext;

import javax.sql.DataSource;

public interface DataSourceDecorator
{

    DataSource decorate(DataSource dataSource, String dataSourceName, MuleContext muleContext);

    boolean appliesTo(DataSource dataSource, MuleContext muleContext);

}
