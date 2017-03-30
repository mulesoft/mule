/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.collectRelativeClasses;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;

import com.google.common.collect.ImmutableSet;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility class which calculates the default set of java package names and resources that a given extension should export in
 * order to properly function
 *
 * @since 4.0
 */
final public class ExportedArtifactsCollector {

  private static final String META_INF_PREFIX = "/META-INF";
  private final Set<String> filteredPackages =
      ImmutableSet.<String>builder().add("java.", "javax.", "org.mule.runtime.", "com.mulesoft.mule.runtime").build();

  private final ExtensionModel extensionModel;
  private final Set<Class<?>> exportedClasses = new LinkedHashSet<>();
  private final ImmutableSet.Builder<String> exportedResources = ImmutableSet.builder();
  private final ClassLoader extensionClassloader;

  /**
   * Creates a new instance
   *
   * @param extensionModel the {@link ExtensionModel model} for the analyzed extension
   */
  public ExportedArtifactsCollector(ExtensionModel extensionModel) {
    this.extensionModel = extensionModel;
    this.extensionClassloader = getClassLoader(extensionModel);
  }

  /**
   * @return The {@link Set} of default resource paths that the extension should export
   */
  public Set<String> getExportedResources() {
    // TODO: remove at Kraan's notice
    addMetaInfResource("");

    addMetaInfResource(EXTENSION_MANIFEST_FILE_NAME);
    collectXmlSupportResources();

    return exportedResources.build();
  }

  /**
   * @return The {@link Set} of default java package names that the extension should export
   */
  public Set<String> getExportedPackages() {
    collectDefault();
    collectManuallyExportedPackages();

    Set<String> exportedPackages = exportedClasses.stream().filter(type -> type.getPackage() != null)
        .map(type -> type.getPackage().getName()).collect(toSet());

    return filterExportedPackages(exportedPackages);
  }

  private void collectXmlSupportResources() {
    XmlDslModel languageModel = extensionModel.getXmlDslModel();

    addMetaInfResource(languageModel.getXsdFileName());
    addMetaInfResource("spring.handlers");
    addMetaInfResource("spring.schemas");

    exportedResources.addAll(extensionModel.getResources());
  }

  private void addMetaInfResource(String resource) {
    exportedResources.add(META_INF_PREFIX + "/" + resource);
  }

  private Set<String> filterExportedPackages(Set<String> exportedPackages) {
    return exportedPackages.stream()
        .filter(packageName -> filteredPackages.stream().noneMatch(packageName::startsWith))
        .collect(toSet());
  }

  private void collectManuallyExportedPackages() {
    extensionModel.getTypes().forEach(c -> exportedClasses.add(getType(c)));
  }

  private void collectDefault() {
    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        exportedClasses.addAll(collectRelativeClasses(model.getType(), extensionClassloader));
      }

      @Override
      public void onOperation(HasOperationModels owner, OperationModel model) {
        collectReturnTypes(model);
        collectExceptionTypes(model);
      }

      @Override
      public void onSource(HasSourceModels owner, SourceModel model) {
        collectReturnTypes(model);
      }

    }.walk(extensionModel);
  }

  private void collectReturnTypes(ComponentModel model) {
    exportedClasses.addAll(collectRelativeClasses(model.getOutput().getType(), extensionClassloader));
    exportedClasses.addAll(collectRelativeClasses(model.getOutputAttributes().getType(), extensionClassloader));
  }

  private void collectExceptionTypes(OperationModel operationModel) {
    operationModel.getModelProperty(ImplementingMethodModelProperty.class)
        .map(ImplementingMethodModelProperty::getMethod)
        .ifPresent(method -> exportedClasses.addAll(asList(method.getExceptionTypes())));
  }
}
