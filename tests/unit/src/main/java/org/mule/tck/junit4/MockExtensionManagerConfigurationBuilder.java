/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static java.util.Collections.emptySet;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;

/**
 * {@link ConfigurationBuilder} used to provide a mock implementation of {@link ExtensionManager} with an empty set of extensions.
 *
 * @since 4.2
 */
public class MockExtensionManagerConfigurationBuilder implements ConfigurationBuilder {

  @Override
  public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {

  }

  @Override
  public void configure(MuleContext muleContext) {
    if (muleContext.getExtensionManager() == null) {
      withContextClassLoader(MockExtensionManagerConfigurationBuilder.class.getClassLoader(), () -> {
        ExtensionManager mockExtensionManager = mock(ExtensionManager.class, RETURNS_DEEP_STUBS.get());
        TestExtensionModel muleExtensionModel = new TestExtensionModel("mule");
        HashSet<ExtensionModel> extensionModels = new HashSet<>();
        extensionModels.add(muleExtensionModel);
        when(mockExtensionManager.getExtensions()).thenReturn(extensionModels);
        muleContext.setExtensionManager(mockExtensionManager);
      });
    }
  }

  private static class TestExtensionModel implements ExtensionModel {

    private final List<SourceModel> sourceModels;
    private final List<ConstructModel> constructModels;
    private XmlDslModel dslModel;
    private final String name;
    private List<OperationModel> operationModels;
    private List<ConfigurationModel> configurationModels;

    public TestExtensionModel() {
      this("extension model default name");
    }

    public TestExtensionModel(String name) {
      this(name, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public TestExtensionModel(String name, List<OperationModel> operationModels, List<ConfigurationModel> configurationModels,
                              ArrayList<SourceModel> sourceModels, ArrayList<ConstructModel> constructModels) {
      this.name = name;
      this.dslModel = XmlDslModel.builder().setPrefix(name).build();
      this.operationModels = operationModels;
      this.configurationModels = configurationModels;
      this.sourceModels = sourceModels;
      this.constructModels = constructModels;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getVersion() {
      return null;
    }

    @Override
    public List<ConfigurationModel> getConfigurationModels() {
      return configurationModels;
    }

    @Override
    public Optional<ConfigurationModel> getConfigurationModel(String name) {
      return configurationModels.stream().filter(x -> x.getName().equals(name)).findFirst();
    }

    @Override
    public List<OperationModel> getOperationModels() {
      return operationModels;
    }

    @Override
    public List<ConnectionProviderModel> getConnectionProviders() {
      return new ArrayList<>();
    }

    @Override
    public List<SourceModel> getSourceModels() {
      return sourceModels;
    }

    @Override
    public Set<ObjectType> getTypes() {
      return new HashSet<>();
    }

    @Override
    public Set<String> getResources() {
      return new HashSet<>();
    }

    @Override
    public Set<String> getPrivilegedPackages() {
      return new HashSet<>();
    }

    @Override
    public Set<String> getPrivilegedArtifacts() {
      return new HashSet<>();
    }

    @Override
    public String getVendor() {
      return "dummy vendor";
    }

    @Override
    public Category getCategory() {
      return null;
    }

    @Override
    public XmlDslModel getXmlDslModel() {
      return dslModel;
    }

    @Override
    public Set<SubTypesModel> getSubTypes() {
      return new HashSet<>();
    }

    @Override
    public Set<ImportedTypeModel> getImportedTypes() {
      return new HashSet<>();
    }

    @Override
    public Set<ErrorModel> getErrorModels() {
      return new HashSet<>();
    }

    @Override
    public Set<NotificationModel> getNotificationModels() {
      return new HashSet<>();
    }

    @Override
    public Optional<ComponentModel> findComponentModel(String componentName) {
      return Optional.empty();
    }

    @Override
    public String getDescription() {
      return "A dummy extension model for testing";
    }

    @Override
    public <T extends ModelProperty> Optional<T> getModelProperty(Class<T> propertyType) {
      return Optional.empty();
    }

    @Override
    public Set<ModelProperty> getModelProperties() {
      return new HashSet<>();
    }

    @Override
    public Set<ExternalLibraryModel> getExternalLibraryModels() {
      return new HashSet<>();
    }

    @Override
    public Optional<ConnectionProviderModel> getConnectionProviderModel(String name) {
      return Optional.empty();
    }

    @Override
    public List<ConstructModel> getConstructModels() {
      return constructModels;
    }

    @Override
    public Optional<ConstructModel> getConstructModel(String name) {
      return Optional.empty();
    }

    @Override
    public Optional<DeprecationModel> getDeprecationModel() {
      return Optional.empty();
    }

    @Override
    public Optional<DisplayModel> getDisplayModel() {
      return Optional.empty();
    }

    @Override
    public List<FunctionModel> getFunctionModels() {
      return new ArrayList<>();
    }

    @Override
    public Optional<FunctionModel> getFunctionModel(String name) {
      return Optional.empty();
    }

    @Override
    public Optional<OperationModel> getOperationModel(String name) {
      return this.getOperationModels().stream().filter(x -> x.getName().equals(name)).findFirst();
    }

    @Override
    public Optional<SourceModel> getSourceModel(String name) {
      return Optional.empty();
    }
  }
}
