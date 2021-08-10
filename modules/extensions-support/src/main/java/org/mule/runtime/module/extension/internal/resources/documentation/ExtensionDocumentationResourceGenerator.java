/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDescriptionsSerializer.SERIALIZER;

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

    List<XmlExtensionElementDocumentation> configs = new ArrayList<>();
    List<XmlExtensionElementDocumentation> connections = new ArrayList<>();
    List<XmlExtensionElementDocumentation> operations = new ArrayList<>();
    List<XmlExtensionElementDocumentation> sources = new ArrayList<>();

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

    private List<XmlExtensionElementDocumentation> createParameterizedElement(ParameterizedModel model) {
      ImmutableList.Builder<XmlExtensionElementDocumentation> builder = ImmutableList.builder();
      XmlExtensionElementDocumentation element = new XmlExtensionElementDocumentation();
      element.setName(model.getName());
      element.setDescription(model.getDescription());
      element.setParameters(model.getAllParameterModels().stream()
          .map(p -> new XmlExtensionParameterDocumentation(p.getName(), p.getDescription()))
          .collect(toList()));
      builder.add(element);
      return builder.build();
    }

    public List<XmlExtensionElementDocumentation> getConfigs() {
      return configs;
    }

    public List<XmlExtensionElementDocumentation> getConnections() {
      return connections;
    }

    public List<XmlExtensionElementDocumentation> getOperations() {
      return operations;
    }

    public List<XmlExtensionElementDocumentation> getSources() {
      return sources;
    }
  }

  private List<XmlExtensionElementDocumentation> getTypesDocumentation(ExtensionModel extensionModel) {
    List<XmlExtensionElementDocumentation> types = new ArrayList<>();

    extensionModel.getTypes().forEach(type -> ExtensionMetadataTypeUtils.getId(type)
        .ifPresent(id -> {
          XmlExtensionElementDocumentation element = new XmlExtensionElementDocumentation();
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


  private XmlExtensionDocumentation getDocumenter(ExtensionModel model,
                                                  List<XmlExtensionElementDocumentation> configs,
                                                  List<XmlExtensionElementDocumentation> connections,
                                                  List<XmlExtensionElementDocumentation> operations,
                                                  List<XmlExtensionElementDocumentation> sources,
                                                  List<XmlExtensionElementDocumentation> types) {
    final XmlExtensionDocumentation documenter = new XmlExtensionDocumentation();
    XmlExtensionElementDocumentation element = new XmlExtensionElementDocumentation();
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
