/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;

import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.api.exception.connection.ConnectionCommitException;
import org.mule.extension.db.api.exception.connection.ConnectionCreationException;
import org.mule.extension.db.api.param.CustomDataType;
import org.mule.extension.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.StructuredDbType;
import org.mule.extension.db.internal.domain.xa.XADbConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.sql.XAConnection;

import org.apache.commons.lang.StringUtils;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a generic DB connection through an URL
 *
 * @since 4.0
 */
public abstract class DbConnectionProvider implements ConnectionProvider<DbConnection>, Initialisable, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbConnectionProvider.class);

  @ConfigName
  private String configName;

  @Inject
  private MuleContext muleContext;

  /**
   * Specifies non-standard custom data types
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private List<CustomDataType> customDataTypes = emptyList();

  /**
   * Provides a way to configure database connection pooling.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED)
  private DbPoolingProfile poolingProfile;

  private DataSourceFactory dataSourceFactory;
  private List<DbType> resolvedCustomTypes = emptyList();
  private JdbcConnectionFactory jdbcConnectionFactory = new JdbcConnectionFactory();
  private DataSource dataSource;

  @Override
  public final DbConnection connect() throws ConnectionException {
    try {
      Connection jdbcConnection = jdbcConnectionFactory.createConnection(dataSource, resolvedCustomTypes);

      DbConnection connection = createDbConnection(jdbcConnection);

      if (jdbcConnection instanceof XAConnection) {
        connection = new XADbConnection(connection, (XAConnection) jdbcConnection);
      }

      return connection;
    } catch (Exception e) {
      throw new ConnectionCreationException(e);
    }
  }

  @Override
  public final void disconnect(DbConnection connection) {
    Connection jdbcConnection = connection.getJdbcConnection();
    try {
      if (jdbcConnection.isClosed()) {
        return;
      }
    } catch (SQLException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Error checking for closed connection while trying to disconnect", e);
      }
      return;
    }
    RuntimeException exception = null;

    try {
      if (!jdbcConnection.getAutoCommit()) {
        jdbcConnection.commit();
      }
    } catch (SQLException e) {
      exception = new ConnectionCommitException(e);
    } finally {
      try {
        connection.release();
      } catch (Exception e) {
        if (exception == null) {
          exception = new ConnectionClosingException(e);
        }
      }
    }

    if (exception != null) {
      throw exception;
    }
  }

  @Override
  public ConnectionValidationResult validate(DbConnection connection) {
    return success();
  }

  @Override
  public final void initialise() throws InitialisationException {
    dataSourceFactory = createDataSourceFactory();
    try {
      dataSource = obtainDataSource();
    } catch (SQLException e) {
      throw new InitialisationException(createStaticMessage("Could not create DataSource for DB config " + configName), e, this);
    }

    resolvedCustomTypes = resolveCustomTypes();
  }

  @Override
  public final void dispose() {
    disposeIfNeeded(dataSourceFactory, LOGGER);
  }

  public abstract java.util.Optional<DataSource> getDataSource();

  public abstract java.util.Optional<DataSourceConfig> getDataSourceConfig();

  protected DbConnection createDbConnection(Connection connection) throws Exception {
    return new DefaultDbConnection(connection, resolvedCustomTypes);
  }

  private DataSource obtainDataSource() throws SQLException {
    final java.util.Optional<DataSource> optionalDataSource = getDataSource();
    final DataSource dataSource;

    if (optionalDataSource.isPresent()) {
      dataSource = optionalDataSource.get();
    } else {
      final DataSourceConfig dataSourceConfig = getDataSourceConfig()
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not create DataSource for DB config, no DataSource or DataSourceConfig has been provided "
              + configName)));
      dataSource = createDataSource(dataSourceConfig);
    }

    return dataSourceFactory.decorateDataSource(dataSource, poolingProfile);
  }

  private DataSource createDataSource(DataSourceConfig dataSourceConfig) throws SQLException {
    return dataSourceFactory.create(dataSourceConfig, poolingProfile);
  }

  protected List<DbType> resolveCustomTypes() {
    return customDataTypes.stream().map(type -> {
      final String name = type.getTypeName();
      final int id = type.getId();
      if (id == Types.ARRAY) {
        return new ArrayResolvedDbType(id, name);
      } else if (id == Types.STRUCT) {
        final String className = type.getClassName();
        if (!StringUtils.isEmpty(className)) {
          Class<?> mappedClass;
          try {
            mappedClass = Class.forName(className);
          } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot find mapped class: " + className);
          }
          return new MappedStructResolvedDbType<>(id, name, mappedClass);
        } else {
          return new StructuredDbType(id, name);
        }
      } else {
        return new ResolvedDbType(id, name);
      }
    })
        .collect(new ImmutableListCollector<>());
  }

  private DataSourceFactory createDataSourceFactory() {
    return new DataSourceFactory(configName, muleContext);
  }

  public DataSource getConfiguredDataSource() {
    return dataSource;
  }
}
