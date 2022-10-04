/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This class provides a skeletal implementation of the {@link ExtensionModel} interface, to minimize the effort required to
 * implement this interface when decorating instances of {@link ExtensionModel}.
 *
 * @since 1.0
 */
public class BaseExtensionModelDecorator implements ExtensionModel {

  private final ExtensionModel decorated;

  /**
   * Creates a new decorator for the provided instance.
   *
   * @param decorated the {@link ExtensionModel} instance to decorate.
   */
  public BaseExtensionModelDecorator(ExtensionModel decorated) {
    this.decorated = decorated;
  }

  @Override
  public String getDescription() {
    return decorated.getDescription();
  }

  @Override
  public <T extends ModelProperty> Optional<T> getModelProperty(Class<T> propertyType) {
    return decorated.getModelProperty(propertyType);
  }

  @Override
  public Set<ModelProperty> getModelProperties() {
    return decorated.getModelProperties();
  }

  @Override
  public String getName() {
    return decorated.getName();
  }

  @Override
  public String getVersion() {
    return decorated.getVersion();
  }

  @Override
  public List<ConfigurationModel> getConfigurationModels() {
    return decorated.getConfigurationModels();
  }

  @Override
  public Optional<ConfigurationModel> getConfigurationModel(String name) {
    return decorated.getConfigurationModel(name);
  }

  @Override
  public List<OperationModel> getOperationModels() {
    return decorated.getOperationModels();
  }

  @Override
  public Optional<OperationModel> getOperationModel(String name) {
    return decorated.getOperationModel(name);
  }

  @Override
  public List<ConnectionProviderModel> getConnectionProviders() {
    return decorated.getConnectionProviders();
  }

  @Override
  public Optional<ConnectionProviderModel> getConnectionProviderModel(String name) {
    return decorated.getConnectionProviderModel(name);
  }

  @Override
  public List<SourceModel> getSourceModels() {
    return decorated.getSourceModels();
  }

  @Override
  public Optional<SourceModel> getSourceModel(String name) {
    return decorated.getSourceModel(name);
  }

  @Override
  public Set<ObjectType> getTypes() {
    return decorated.getTypes();
  }

  @Override
  public Set<String> getResources() {
    return decorated.getResources();
  }

  @Override
  public Set<String> getPrivilegedPackages() {
    return decorated.getPrivilegedPackages();
  }

  @Override
  public Set<String> getPrivilegedArtifacts() {
    return decorated.getPrivilegedArtifacts();
  }

  @Override
  public String getVendor() {
    return decorated.getVendor();
  }

  @Override
  public Category getCategory() {
    return decorated.getCategory();
  }

  @Override
  public XmlDslModel getXmlDslModel() {
    return decorated.getXmlDslModel();
  }

  @Override
  public Set<SubTypesModel> getSubTypes() {
    return decorated.getSubTypes();
  }

  @Override
  public Set<ImportedTypeModel> getImportedTypes() {
    return decorated.getImportedTypes();
  }

  @Override
  public Set<ErrorModel> getErrorModels() {
    return decorated.getErrorModels();
  }

  @Override
  public Set<NotificationModel> getNotificationModels() {
    return decorated.getNotificationModels();
  }

  @Override
  public Optional<ComponentModel> findComponentModel(String componentName) {
    return decorated.findComponentModel(componentName);
  }

  @Override
  public Optional<ArtifactCoordinates> getArtifactCoordinates() {
    return decorated.getArtifactCoordinates();
  }

  @Override
  public Set<ExternalLibraryModel> getExternalLibraryModels() {
    return decorated.getExternalLibraryModels();
  }

  @Override
  public List<ConstructModel> getConstructModels() {
    return decorated.getConstructModels();
  }

  @Override
  public Optional<ConstructModel> getConstructModel(String name) {
    return decorated.getConstructModel(name);
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return decorated.getDeprecationModel();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return decorated.getDisplayModel();
  }

  @Override
  public List<FunctionModel> getFunctionModels() {
    return decorated.getFunctionModels();
  }

  @Override
  public Optional<FunctionModel> getFunctionModel(String name) {
    return decorated.getFunctionModel(name);
  }
}
