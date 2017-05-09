/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.extension.db.api.exception.connection.DbError.CANNOT_LOAD_DRIVER;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.api.exception.connection.ConnectionCommitException;
import org.mule.extension.db.api.exception.connection.ConnectionCreationException;
import org.mule.extension.db.api.exception.connection.DbError;
import org.mule.extension.db.api.param.ColumnType;
import org.mule.extension.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.extension.db.internal.domain.type.ClobResolvedDataType;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.StructDbType;
import org.mule.extension.db.internal.domain.xa.XADbConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tx.MuleXaObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.sql.XAConnection;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * Creates a generic DB connection through an URL
 *
 * @since 4.0
 */
public abstract class DbConnectionProvider implements ConnectionProvider<DbConnection>, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DbConnectionProvider.class);
  public static final String DRIVER_FILE_NAME_PATTERN = "(.*)\\.jar";
  protected static final String CONNECTION_ERROR_MESSAGE = "Could not obtain connection from data source";
  private static final String ERROR_TRYING_TO_LOAD_DRIVER = "Error trying to load driver";


  @ConfigName
  private String configName;

  @Inject
  private MuleContext muleContext;

  /**
   * Provides a way to configure database connection pooling.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB)
  private DbPoolingProfile poolingProfile;

  /**
   * Specifies non-standard column types
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private List<ColumnType> columnTypes = emptyList();


  private DataSourceFactory dataSourceFactory;
  private List<DbType> resolvedCustomTypes = emptyList();
  private JdbcConnectionFactory jdbcConnectionFactory = createJdbcConnectionFactory();

  /**
   * Creates the {@link JdbcConnectionFactory} to use on this provider
   *
   * @return a non null provider.
   */
  protected JdbcConnectionFactory createJdbcConnectionFactory() {
    return new JdbcConnectionFactory();
  }

  private DataSource dataSource;

  private java.util.Optional<DbError> getDbErrorType(SQLException e) {
    String message = e.getMessage();
    if (message.contains(ERROR_TRYING_TO_LOAD_DRIVER)) {
      return of(CANNOT_LOAD_DRIVER);
    }
    return getDbVendorErrorType(e);
  }

  protected java.util.Optional<DbError> getDbVendorErrorType(SQLException e) {
    return empty();
  }

  @Override
  public final DbConnection connect() throws ConnectionException {
    try {
      Connection jdbcConnection = jdbcConnectionFactory.createConnection(dataSource, resolvedCustomTypes);
      java.util.Optional<XAConnection> optionalXaConnection = getXaConnection(jdbcConnection);

      DbConnection connection = createDbConnection(jdbcConnection);

      if (optionalXaConnection.isPresent()) {
        connection = new XADbConnection(connection, optionalXaConnection.get());
      }

      return connection;
    } catch (ConnectionException e) {
      throw e;
    } catch (Exception e) {
      throw handleSQLConnectionException(e);
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
    return columnTypes.stream().map(type -> {
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
          return new StructDbType(id, name);
        }
      } else if (id == Types.CLOB) {
        return new ClobResolvedDataType(id, name);
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

  private boolean isXaConnection(Connection jdbcConnection) {
    return jdbcConnection instanceof MuleXaObject && ((MuleXaObject) jdbcConnection).getTargetObject() instanceof XAConnection;
  }

  private java.util.Optional<XAConnection> getXaConnection(Connection jdbcConnection) {
    return isXaConnection(jdbcConnection)
        ? java.util.Optional.of((XAConnection) ((MuleXaObject) jdbcConnection).getTargetObject())
        : empty();
  }

  private ConnectionException handleSQLConnectionException(Exception e) {
    java.util.Optional<DbError> dbError = empty();
    if (e instanceof SQLException) {
      dbError = getDbErrorType((SQLException) e);
    }

    return dbError
        .map(errorType -> new ConnectionCreationException(CONNECTION_ERROR_MESSAGE, e, errorType))
        .orElse(new ConnectionCreationException(CONNECTION_ERROR_MESSAGE, e));
  }
}
