/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.internal.ExtensionXmlSerializer.serialize;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_DESCRIPTIONS_FILE_NAME;
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

import com.google.common.collect.ImmutableList;

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
    String documenter = serialize(walker.getDocumenter(extensionModel));
    return Optional.of(new GeneratedResource(EXTENSION_DESCRIPTIONS_FILE_NAME, documenter.getBytes()));
  }

  private class ExtensionDocumenterWalker extends ExtensionWalker {

    ImmutableList.Builder<XmlExtensionElementDocumenter> elements = ImmutableList.builder();

    @Override
    protected void onConfiguration(ConfigurationModel model) {
      createParameterizedElement(model);
    }

    @Override
    protected void onOperation(HasOperationModels owner, OperationModel model) {
      createParameterizedElement(model);
    }

    @Override
    protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
      createParameterizedElement(model);
    }

    @Override
    protected void onSource(HasSourceModels owner, SourceModel model) {
      createParameterizedElement(model);
    }

    private void createParameterizedElement(ParameterizedModel model) {
      XmlExtensionElementDocumenter element = new XmlExtensionElementDocumenter();
      element.setName(model.getName());
      element.setDescription(model.getDescription());
      element.setParameters(model.getAllParameterModels().stream()
          .map(p -> new XmlExtensionParameterDocumenter(p.getName(), p.getDescription()))
          .collect(toList()));
      elements.add(element);
    }

    private XmlExtensionDocumenter getDocumenter(ExtensionModel model) {
      final XmlExtensionDocumenter documenter = new XmlExtensionDocumenter();
      XmlExtensionElementDocumenter element = new XmlExtensionElementDocumenter();
      element.setName(model.getName());
      element.setDescription(model.getDescription());
      element.setParameters(emptyList());
      elements.add(element);
      documenter.setElements(elements.build());
      return documenter;
    }
  }
}
