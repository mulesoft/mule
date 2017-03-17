/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.module.extension.internal.capability.xml.schema.builder.SchemaBuilder;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NamespaceFilter;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Generator class that takes a {@link ExtensionModel} with a {@link XmlDslModel} and returns a XSD schema as a String
 *
 * @since 3.7.0
 */
public class SchemaGenerator {

  private void validate(ExtensionModel extensionModel, XmlDslModel xmlDslModel) {
    checkArgument(extensionModel != null, "extension cannot be null");
    checkArgument(xmlDslModel != null, "xml model property cannot be null");
    checkState(!StringUtils.isBlank(xmlDslModel.getPrefix()), "xml model property cannot provide a blank namespace");
  }

  public String generate(ExtensionModel extensionModel, XmlDslModel xmlDslModel, DslResolvingContext dslContext) {
    validate(extensionModel, xmlDslModel);
    SchemaBuilder schemaBuilder = SchemaBuilder.newSchema(extensionModel, xmlDslModel, dslContext);

    new IdempotentExtensionWalker() {

      @Override
      public void onConfiguration(ConfigurationModel model) {
        schemaBuilder.registerConfigElement(model);
      }

      @Override
      public void onOperation(OperationModel model) {
        schemaBuilder.registerOperation(model);
      }

      @Override
      public void onConnectionProvider(ConnectionProviderModel model) {
        schemaBuilder.registerConnectionProviderElement(model);
      }

      @Override
      public void onSource(SourceModel model) {
        schemaBuilder.registerMessageSource(model);
      }
    }.walk(extensionModel);

    schemaBuilder.registerEnums();

    return renderSchema(schemaBuilder.build());
  }

  private String renderSchema(Schema schema) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Schema.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      NamespaceFilter outFilter = new NamespaceFilter(CORE_PREFIX, DEFAULT_NAMESPACE_URI_MASK, true);
      OutputFormat format = new OutputFormat();
      format.setIndent(true);
      format.setNewlines(true);

      StringWriter sw = new StringWriter();
      XMLWriter writer = new XMLWriter(sw, format);
      outFilter.setContentHandler(writer);
      marshaller.marshal(schema, outFilter);
      return sw.toString();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }

  }
}
