/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import java.util.Optional;

import javax.sql.DataSource;

import static java.util.Optional.empty;

/**
 * Contract for classes considered as database connection parameter
 *
 * @since 4.0
 */
public interface DbConnectionParameters {

  /**
   * @return an {@link Optional} {@link DataSource}
   */
  default Optional<DataSource> getDataSource() {
    return empty();
  }

  /**
   * @return an {@link Optional} {@link DataSourceConfig}
   */
  default Optional<DataSourceConfig> getDataSourceConfig() {
    return empty();
  }
}
