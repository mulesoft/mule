/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.module.extension.api.resources.documentation.ExtensionDescriptionsSerializer.SERIALIZER;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;

import org.mule.metadata.api.annotation.DescriptionAnnotation;
import org.mule.metadata.api.model.MetadataType;
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
    return of(new GeneratedResource(true, "META-INF/" + SERIALIZER.getFileName(extensionModel.getName()), documenter.getBytes()));
  }

  private class ExtensionDocumenterWalker extends ExtensionWalker {

    List<DefaultXmlExtensionElementDocumentation> configs = new ArrayList<>();
    List<DefaultXmlExtensionElementDocumentation> connections = new ArrayList<>();
    List<DefaultXmlExtensionElementDocumentation> operations = new ArrayList<>();
    List<DefaultXmlExtensionElementDocumentation> sources = new ArrayList<>();

    @Override
    protected void onConfiguration(ConfigurationModel model) {
      configs.add(createParameterizedElement(model));
    }

    @Override
    protected void onOperation(HasOperationModels owner, OperationModel model) {
      operations.add(createParameterizedElement(model));
    }

    @Override
    protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
      connections.add(createParameterizedElement(model));
    }

    @Override
    protected void onSource(HasSourceModels owner, SourceModel model) {
      sources.add(createParameterizedElement(model));
    }

    private DefaultXmlExtensionElementDocumentation createParameterizedElement(ParameterizedModel model) {
      DefaultXmlExtensionElementDocumentation element = new DefaultXmlExtensionElementDocumentation();
      element.setName(model.getName());
      element.setDescription(model.getDescription());
      element.setParameters(model.getAllParameterModels().stream()
          .map(p -> new DefaultXmlExtensionParameterDocumentation(p.getName(), p.getDescription()))
          .toList());

      return element;
    }

    public List<DefaultXmlExtensionElementDocumentation> getConfigs() {
      return configs;
    }

    public List<DefaultXmlExtensionElementDocumentation> getConnections() {
      return connections;
    }

    public List<DefaultXmlExtensionElementDocumentation> getOperations() {
      return operations;
    }

    public List<DefaultXmlExtensionElementDocumentation> getSources() {
      return sources;
    }
  }

  private List<DefaultXmlExtensionElementDocumentation> getTypesDocumentation(ExtensionModel extensionModel) {
    return extensionModel.getTypes()
        .stream()
        .map(type -> ExtensionMetadataTypeUtils.getId(type)
            .map(id -> {
              DefaultXmlExtensionElementDocumentation element = new DefaultXmlExtensionElementDocumentation();
              element.setName(id);
              element.setDescription(getDescription(type));
              element.setParameters(type.getFields().stream()
                  .map(f -> new DefaultXmlExtensionParameterDocumentation(getAlias(f), getDescription(f)))
                  .toList());

              return element;
            }))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private String getDescription(MetadataType type) {
    return type.getAnnotation(DescriptionAnnotation.class)
        .map(DescriptionAnnotation::getValue)
        .orElse("");
  }

  private DefaultXmlExtensionDocumentation getDocumenter(ExtensionModel model,
                                                         List<DefaultXmlExtensionElementDocumentation> configs,
                                                         List<DefaultXmlExtensionElementDocumentation> connections,
                                                         List<DefaultXmlExtensionElementDocumentation> operations,
                                                         List<DefaultXmlExtensionElementDocumentation> sources,
                                                         List<DefaultXmlExtensionElementDocumentation> types) {
    final DefaultXmlExtensionDocumentation documenter = new DefaultXmlExtensionDocumentation();
    DefaultXmlExtensionElementDocumentation element = new DefaultXmlExtensionElementDocumentation();
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
