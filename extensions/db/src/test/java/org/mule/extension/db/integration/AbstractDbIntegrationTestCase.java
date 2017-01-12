/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.endsWith;
import static org.apache.commons.lang.StringUtils.replace;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.mule.test.runner.RunnerDelegateTo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@ArtifactClassLoaderRunnerConfig(exportPluginClasses = {DbConnectionProvider.class},
    sharedRuntimeLibs = {"org.apache.derby:derby"})
public abstract class AbstractDbIntegrationTestCase extends MuleArtifactFunctionalTestCase {

  @Parameterized.Parameter(0)
  public String dataSourceConfigResource;
  @Parameterized.Parameter(1)
  public AbstractTestDatabase testDatabase;
  @Parameterized.Parameter(2)
  public DbTestUtil.DbType dbType;

  protected final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  protected final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Before
  public void configDB() throws SQLException {
    testDatabase.createDefaultDatabaseConfig(getDefaultDataSource());
  }

  protected DataSource getDefaultDataSource() {
    return getDefaultDataSource("dbConfig");
  }

  protected DataSource getDefaultDataSource(String configName) {
    try {
      ConfigurationProvider configurationProvider = muleContext.getRegistry().get(configName);
      ConnectionProviderWrapper<DbConnection> connectionProviderWrapper =
          (ConnectionProviderWrapper<DbConnection>) configurationProvider
              .get(testEvent())
              .getConnectionProvider().get();

      return ((DbConnectionProvider) connectionProviderWrapper.getDelegate()).getConfiguredDataSource();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected final String[] getConfigFiles() {
    StringBuilder builder = new StringBuilder();

    builder.append(getDatasourceConfigurationResource());

    for (String resource : getFlowConfigurationResources()) {
      if (builder.length() != 0) {
        builder.append(",");
      }

      builder.append(resource);
    }

    return builder.toString().split(",");
  }

  protected final String getDatasourceConfigurationResource() {
    return dataSourceConfigResource;
  }

  protected abstract String[] getFlowConfigurationResources();

  protected void assertPlanetRecordsFromQuery(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();
    List<Record> records = new ArrayList<>(names.length);

    for (String name : names) {
      addCondition(conditionBuilder, name);
      records.add(new Record(new Field("NAME", replace(name, "'", ""))));
    }

    List<Map<String, String>> result =
        selectData(format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());

    assertRecords(result, records.toArray(new Record[0]));
  }

  protected void assertAffectedRows(StatementResult result, int expected) {
    assertThat(result.getAffectedRows(), is(expected));
  }

  protected void assertDeletedPlanetRecords(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();

    for (String name : names) {
      addCondition(conditionBuilder, name);
    }

    List<Map<String, String>> result =
        selectData(format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());
    assertThat(result.size(), equalTo(0));
  }

  private String addCondition(StringBuilder conditionBuilder, String name) {
    if (conditionBuilder.length() != 0) {
      conditionBuilder.append(",");
    }

    if (!(startsWith(name, "'") && endsWith(name, "'"))) {
      name = format("'%s'", name);
    }

    conditionBuilder.append(name);
    return name;
  }

  protected Map<String, Object> runProcedure(String flowName) throws Exception {
    return runProcedure(flowName, null);
  }

  protected Map<String, Object> runProcedure(String flowName, Object payload) throws Exception {
    FlowRunner runner = flowRunner(flowName);
    if (payload != null) {
      runner.withPayload(payload);
    }

    Message response = runner.run().getMessage();
    assertThat(response.getPayload().getValue(), is(instanceOf(Map.class)));
    return (Map<String, Object>) response.getPayload().getValue();
  }

  protected MetadataResult<ComponentMetadataDescriptor<OperationModel>> getMetadata(String flow, String query)
      throws RegistrationException {
    MetadataService metadataService = muleContext.getRegistry().lookupObject(MuleMetadataService.class);
    return metadataService.getOperationMetadata(new ProcessorId(flow, "0"), newKey(query).build());
  }

  protected MetadataType getInputMetadata(String flow, String query) throws RegistrationException {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getMetadata(flow, query);

    assertThat(metadata.isSuccess(), is(true));
    return metadata.get().getModel().getAllParameterModels().stream()
        .filter(p -> p.getName().equals("inputParameters"))
        .findFirst().get().getType();
  }

  protected void assertFieldOfType(ObjectType record, String name, MetadataType type) {
    Optional<ObjectFieldType> field = record.getFieldByName(name);
    assertThat(field.isPresent(), is(true));
    assertThat(field.get().getValue(), equalTo(type));
  }

  protected void assertOutputPayload(MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata, MetadataType type) {
    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getModel().getOutput().getType(), is(type));
  }

  protected MetadataType getParameterValuesMetadata(String flow, String query) throws RegistrationException {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getMetadata(flow, query);
    assertThat(metadata.isSuccess(), is(true));
    return metadata.get().getModel().getAllParameterModels().stream()
        .filter(p -> p.getName().equals("parameterValues")).findFirst().get().getType();
  }

}
