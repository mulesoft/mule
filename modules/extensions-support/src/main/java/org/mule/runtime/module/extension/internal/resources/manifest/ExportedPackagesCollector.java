/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.collectRelativeClassesAsString;

import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.ExportedClassNamesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;

/**
 * Utility class which calculates the default set of java package names and resources that a given extension should export in
 * order to properly function
 *
 * @since 4.0
 */
final public class ExportedPackagesCollector {

  private final Set<String> filteredPackages =
      ImmutableSet.<String>builder().add("java.", "javax.", "org.mule.runtime.", "com.mulesoft.mule.runtime").build();

  private final ExtensionModel extensionModel;
  private final Set<String> exportedClasses = new LinkedHashSet<>();

  private final Set<String> privilegedExportedPackages = new LinkedHashSet<>();
  private final Set<String> privilegedArtifacts = new LinkedHashSet<>();
  private final ClassPackageFinder packageFinder;

  /**
   * Creates a new instance
   *
   * @param extensionModel the {@link ExtensionModel model} for the analyzed extension
   */
  public ExportedPackagesCollector(ExtensionModel extensionModel) {
    this(extensionModel, new DefaultClassPackageFinder());
  }

  /**
   * Creates a new instance
   *
   * @param extensionModel the {@link ExtensionModel model} for the analyzed extension
   */
  public ExportedPackagesCollector(ExtensionModel extensionModel, ClassPackageFinder packageFinder) {
    this.extensionModel = extensionModel;
    this.packageFinder = packageFinder;
  }

  /**
   * @return The {@link Set} of default resource paths that the extension should export
   */
  public Set<String> getExportedResources() {
    return extensionModel.getResources();
  }

  /**
   * @return The {@link Set} of default java package names that the extension should export
   */
  public Set<String> getExportedPackages() {
    collectDefault();
    collectManuallyExportedPackages();

    Set<String> exportedPackages = exportedClasses.stream()
        .map(packageFinder::packageFor)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toSet());

    return filterExportedPackages(exportedPackages);
  }

  /**
   * @return A {@link Map} of default java package names that the extension should export and indicates which are the classes that
   *         makes the package to be exported.
   */
  public Map<String, Collection<String>> getDetailedExportedPackages() {
    collectDefault();
    collectManuallyExportedPackages();
    HashMultimap<String, String> exportedPackages = HashMultimap.create();

    exportedClasses.stream()
        .map(clazz -> new Pair<>(packageFinder.packageFor(clazz).orElse(""), clazz))
        .filter(pair -> !isBlank(pair.getFirst()))
        .filter(pair -> filteredPackages.stream().noneMatch(filteredPackage -> pair.getFirst().startsWith(filteredPackage)))
        .forEach(pair -> exportedPackages.put(pair.getFirst(), pair.getSecond()));

    return exportedPackages.asMap();
  }

  /**
   * @return The {@link Set} of Java package names that the extension should export on the privileged API
   */
  public Set<String> getPrivilegedExportedPackages() {
    privilegedExportedPackages.addAll(extensionModel.getPrivilegedPackages());
    return filterExportedPackages(privilegedExportedPackages);
  }

  /**
   * @return The {@link Set} of artifact IDs that the extension should grant access to the privileged API. Each artifact is
   *         defined using Maven's groupId:artifactId
   */
  public Set<String> getPrivilegedArtifacts() {
    privilegedArtifacts.addAll(extensionModel.getPrivilegedArtifacts());

    return privilegedArtifacts;
  }

  private Set<String> filterExportedPackages(Set<String> exportedPackages) {
    return exportedPackages.stream()
        .filter(packageName -> filteredPackages.stream().noneMatch(packageName::startsWith))
        .collect(toSet());
  }

  private void collectManuallyExportedPackages() {
    extensionModel.getTypes().forEach(t -> getId(t).ifPresent(exportedClasses::add));
    extensionModel.getModelProperty(ExportedClassNamesModelProperty.class)
        .ifPresent(p -> exportedClasses.addAll(p.getExportedClassNames()));
  }

  private void collectDefault() {
    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        exportedClasses.addAll(collectRelativeClassesAsString(model.getType()));
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

  private void collectReturnTypes(ConnectableComponentModel model) {
    exportedClasses.addAll(collectRelativeClassesAsString(model.getOutput().getType()));
    exportedClasses.addAll(collectRelativeClassesAsString(model.getOutputAttributes().getType()));
  }

  private void collectExceptionTypes(OperationModel operationModel) {
    operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class)
        .map(ExtensionOperationDescriptorModelProperty::getOperationElement)
        .ifPresent(method -> exportedClasses.addAll(method.getExceptionTypes().stream().map(Type::getTypeName).collect(toSet())));
  }
}
