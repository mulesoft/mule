/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import org.mule.extension.db.api.StatementStreamingResultSetCloser;
import org.mule.extension.db.api.param.BulkQueryDefinition;
import org.mule.extension.db.api.param.CustomDataType;
import org.mule.extension.db.api.param.JdbcType;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.StoredProcedureCall;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.extension.db.internal.domain.connection.derby.DerbyConnectionProvider;
import org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionProvider;
import org.mule.extension.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.extension.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.extension.db.internal.domain.type.MetadataDbTypeManager;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.StaticDbTypeManager;
import org.mule.extension.db.internal.operation.BulkOperations;
import org.mule.extension.db.internal.operation.DdlOperations;
import org.mule.extension.db.internal.operation.DmlOperations;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Connector for connecting to relation Databases through the JDBC API
 *
 * @since 4.0
 */
@Extension(name = "Database", description = "Connector for connecting to relation Databases through the JDBC API")
@Operations({DmlOperations.class, DdlOperations.class, BulkOperations.class})
@ConnectionProviders({DbConnectionProvider.class, DerbyConnectionProvider.class, MySqlConnectionProvider.class})
@Xml(namespace = "dbn")
@Export(classes = {StatementStreamingResultSetCloser.class, QueryDefinition.class, StoredProcedureCall.class,
    BulkQueryDefinition.class},
    resources = {"/META-INF/services/org/mule/runtime/core/config/registry-bootstrap.properties"})
public class DbConnector implements Initialisable {

  /**
   * Specifies non-standard custom data types
   */
  @Parameter
  @Optional
  private List<CustomDataType> customDataTypes = new LinkedList<>();

  private DbTypeManager typeManager;

  @Override
  public void initialise() throws InitialisationException {
    typeManager = createBaseTypeManager();
  }

  public DbTypeManager getTypeManager() {
    return typeManager;
  }

  private DbTypeManager createBaseTypeManager() {
    List<DbTypeManager> typeManagers = new ArrayList<>();

    typeManagers.add(new MetadataDbTypeManager());

    if (customDataTypes.size() > 0) {
      typeManagers.add(new StaticDbTypeManager(getCustomTypes()));
    }

    typeManagers.add(new StaticDbTypeManager(JdbcType.getAllTypes()));

    return new CompositeDbTypeManager(typeManagers);
  }

  private List<DbType> getCustomTypes() {
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
          return new ResolvedDbType(id, name);
        }
      } else {
        return new ResolvedDbType(id, name);
      }
    })
        .collect(new ImmutableListCollector<>());
  }
}
