/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;


import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.SchemaResourceFactory;

/**
 * Implementation of {@link AbstractXmlResourceFactory} which generates the extension's XSD schema
 *
 * @since 4.0
 */
public class SchemaXmlResourceFactory extends AbstractXmlResourceFactory implements SchemaResourceFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  protected GeneratedResource generateXmlResource(ExtensionModel extensionModel, XmlDslModel xmlDslModel,
                                                  DslResolvingContext context) {

    String schema = new SchemaGenerator().generate(extensionModel, xmlDslModel, context);
    return new GeneratedResource(xmlDslModel.getXsdFileName(), schema.getBytes());
  }
}
