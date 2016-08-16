/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.xa;

import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.runtime.core.api.MuleContext;

import java.util.Collection;
import java.util.LinkedList;

import javax.sql.DataSource;

/**
 * Composes multiple {@link DataSourceDecorator} instances
 */
public class CompositeDataSourceDecorator implements DataSourceDecorator {

  private final LinkedList<DataSourceDecorator> decorators = new LinkedList<>();

  public CompositeDataSourceDecorator(Collection<DataSourceDecorator> decorators) {
    this.decorators.addAll(decorators);
  }

  @Override
  public DataSource decorate(DataSource dataSource, String dataSourceName, DbPoolingProfile dbPoolingProfile,
                             MuleContext muleContext) {
    for (DataSourceDecorator decorator : decorators) {
      if (decorator.appliesTo(dataSource, muleContext)) {
        return decorator.decorate(dataSource, dataSourceName, dbPoolingProfile, muleContext);
      }
    }
    return dataSource;
  }

  @Override
  public boolean appliesTo(DataSource dataSource, MuleContext muleContext) {
    return true;
  }
}
