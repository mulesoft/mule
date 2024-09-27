/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDescriptionsSerializer.SERIALIZER;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.annotation.DescriptionAnnotation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionDocumentationApi;
import org.mule.runtime.module.extension.privileged.resources.documentation.XmlExtensionElementDocumentationApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

/**
 * A {@link GeneratedResourceFactory} which generates an XML file with all the {@link ExtensionModel} elements and it's
 * corresponding descriptions so they don't get lost once the extension it's packaged.
 *
 * @since 4.0
 */
public class ExtensionDocumentationResourceGenerator implements GeneratedResourceFactory {

  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    ExtensionDocumenterWalker walker = new ExtensionDocumenterWalker();
    walker.walk(extensionModel);

    String documenter = SERIALIZER.serialize(getDocumenter(extensionModel,
                                                           walker.getConfigs(),
                                                           walker.getConnections(),
                                                           walker.getOperations(),
                                                           walker.getSources(),
                                                           getTypesDocumentation(extensionModel)));
    return Optional.of(new GeneratedResource(SERIALIZER.getFileName(extensionModel.getName()), documenter.getBytes()));
  }

  private class ExtensionDocumenterWalker extends ExtensionWalker {

    List<XmlExtensionElementDocumentationApi> configs = new ArrayList<>();
    List<XmlExtensionElementDocumentationApi> connections = new ArrayList<>();
    List<XmlExtensionElementDocumentationApi> operations = new ArrayList<>();
    List<XmlExtensionElementDocumentationApi> sources = new ArrayList<>();

    @Override
    protected void onConfiguration(ConfigurationModel model) {
      configs.addAll(createParameterizedElement(model));
    }

    @Override
    protected void onOperation(HasOperationModels owner, OperationModel model) {
      operations.addAll(createParameterizedElement(model));
    }

    @Override
    protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
      connections.addAll(createParameterizedElement(model));
    }

    @Override
    protected void onSource(HasSourceModels owner, SourceModel model) {
      sources.addAll(createParameterizedElement(model));
    }

    private List<XmlExtensionElementDocumentationApi> createParameterizedElement(ParameterizedModel model) {
      ImmutableList.Builder<XmlExtensionElementDocumentationApi> builder = ImmutableList.builder();
      XmlExtensionElementDocumentationApi element = new XmlExtensionElementDocumentation();
      element.setName(model.getName());
      element.setDescription(model.getDescription());
      element.setParameters(model.getAllParameterModels().stream()
          .map(p -> new XmlExtensionParameterDocumentation(p.getName(), p.getDescription()))
          .collect(toList()));
      builder.add(element);
      return builder.build();
    }

    public List<XmlExtensionElementDocumentationApi> getConfigs() {
      return configs;
    }

    public List<XmlExtensionElementDocumentationApi> getConnections() {
      return connections;
    }

    public List<XmlExtensionElementDocumentationApi> getOperations() {
      return operations;
    }

    public List<XmlExtensionElementDocumentationApi> getSources() {
      return sources;
    }
  }

  private List<XmlExtensionElementDocumentationApi> getTypesDocumentation(ExtensionModel extensionModel) {
    List<XmlExtensionElementDocumentationApi> types = new ArrayList<>();

    extensionModel.getTypes().forEach(type -> ExtensionMetadataTypeUtils.getId(type)
        .ifPresent(id -> {
          XmlExtensionElementDocumentationApi element = new XmlExtensionElementDocumentation();
          element.setName(id);
          element.setDescription(type.getAnnotation(DescriptionAnnotation.class)
              .map(DescriptionAnnotation::getValue).orElse(""));
          element.setParameters(type.getFields().stream()
              .map(f -> new XmlExtensionParameterDocumentation(getAlias(f),
                                                               f.getAnnotation(DescriptionAnnotation.class)
                                                                   .map(DescriptionAnnotation::getValue).orElse("")))
              .collect(toList()));

          types.add(element);
        }));
    return types;
  }


  private XmlExtensionDocumentationApi getDocumenter(ExtensionModel model,
                                                     List<XmlExtensionElementDocumentationApi> configs,
                                                     List<XmlExtensionElementDocumentationApi> connections,
                                                     List<XmlExtensionElementDocumentationApi> operations,
                                                     List<XmlExtensionElementDocumentationApi> sources,
                                                     List<XmlExtensionElementDocumentationApi> types) {
    final XmlExtensionDocumentationApi documenter = new XmlExtensionDocumentation();
    XmlExtensionElementDocumentationApi element = new XmlExtensionElementDocumentation();
    element.setName(model.getName());
    element.setDescription(model.getDescription());
    element.setParameters(emptyList());
    documenter.setExtension(element);
    documenter.setConfigs(configs);
    documenter.setConnections(connections);
    documenter.setOperation(operations);
    documenter.setSources(sources);
    documenter.setTypes(types);
    return documenter;
  }
}
