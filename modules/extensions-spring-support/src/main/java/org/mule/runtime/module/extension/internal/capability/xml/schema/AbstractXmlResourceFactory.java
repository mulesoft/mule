/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.DslResourceFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Base class for {@link GeneratedResourceFactory} implementations which generate resources needed to support configuring
 * extensions through XML
 *
 * @since 4.0
 */
abstract class AbstractXmlResourceFactory implements DslResourceFactory {

  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    Set<ImportedTypeModel> importedTypes = extensionModel.getImportedTypes();
    DslResolvingContext dslContext = importedTypes.isEmpty()
        ? new NullDslResolvingContext()
        : new ClasspathBasedDslContext(extensionModel.getModelProperty(ImplementingTypeModelProperty.class)
            .map(mp -> mp.getType().getClassLoader())
            .orElse(Thread.currentThread().getContextClassLoader()));

    return generateResource(extensionModel, dslContext);
  }

  /**
   * Tests the given {@code extensionModel} to be enriched with the {@link XmlDslModel}. If the property is present, then it
   * delegates into {@link #generateXmlResource(ExtensionModel, XmlDslModel, DslResolvingContext)}.
   * <p>
   * Otherwise, it returns {@link Optional#empty()}
   *
   * @param extensionModel the {@link ExtensionModel} that requires the resource
   * @return an {@link Optional} {@link GeneratedResource}
   */
  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel, DslResolvingContext context) {
    XmlDslModel languageModel = extensionModel.getXmlDslModel();
    return languageModel == null ? empty() : of(generateXmlResource(extensionModel, languageModel, context));
  }

  /**
   * Delegate method which should contain the actual logic to generate the resource
   *
   * @param extensionModel the {@link ExtensionModel} that requires the resource
   * @param xmlDslModel    the extension's {@link XmlDslModel}
   * @param context
   * @return a {@link GeneratedResource}
   */
  protected abstract GeneratedResource generateXmlResource(ExtensionModel extensionModel, XmlDslModel xmlDslModel,
                                                           DslResolvingContext context);

  /**
   * Escapes special characters for the {@link Properties} class that Spring uses to parse the bundle.
   *
   * @param content the content to be escaped
   * @return the escaped content
   */
  protected String escape(String content) {
    return content.replaceAll(":", "\\\\:");
  }

}
