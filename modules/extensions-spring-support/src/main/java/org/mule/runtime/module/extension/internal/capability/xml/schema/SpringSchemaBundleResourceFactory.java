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

/**
 * Generates a Spring bundle file which links the extension's namespace to its schema file
 *
 * @since 4.0
 * @deprecated Will be removed as soon as MULE-9865 is fixed. Do not use.
 */
@Deprecated
public class SpringSchemaBundleResourceFactory extends AbstractXmlResourceFactory {

  static final String GENERATED_FILE_NAME = "spring.schemas";
  static final String BUNDLE_MASK = "%s/%s/%s=META-INF/%s\n";

  /**
   * {@inheritDoc}
   */
  @Override
  protected GeneratedResource generateXmlResource(ExtensionModel extensionModel, XmlDslModel xmlDslModel,
                                                  DslResolvingContext context) {
    StringBuilder contentBuilder = new StringBuilder();
    contentBuilder.append(getSpringSchemaBundle(xmlDslModel, xmlDslModel.getSchemaVersion()));
    contentBuilder.append(getSpringSchemaBundle(xmlDslModel, "current"));

    return new GeneratedResource(GENERATED_FILE_NAME, contentBuilder.toString().getBytes());
  }

  private String getSpringSchemaBundle(XmlDslModel xmlProperty, String version) {
    String filename = xmlProperty.getXsdFileName();
    return escape(String.format(BUNDLE_MASK, xmlProperty.getNamespace(), version, filename, filename));
  }
}
