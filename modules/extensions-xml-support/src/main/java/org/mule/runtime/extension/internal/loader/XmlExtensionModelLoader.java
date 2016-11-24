/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.Describer;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.runtime.ExtensionFactory;
import org.mule.runtime.extension.internal.introspection.describer.XmlBasedDescriber;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;

import java.util.Map;

/**
 * Implementation of {@link ExtensionModelLoader} for those plugins that have an ID that matches with {@link #DESCRIBER_ID},
 * which implies that are extensions built through XML.
 * <p/>
 * This class will be responsible of picking up the proper {@link Describer} which, in this XML scenario, will the
 * {@link XmlBasedDescriber} class.
 *
 * @since 4.0
 */
public class XmlExtensionModelLoader implements ExtensionModelLoader {

  /**
   * Attribute to look for in the parametrized attributes picked up from the descriptor.
   */
  public static final String RESOURCE_XML = "resource-xml";

  /**
   * The ID which represents {@code this} {@link Describer} that will be used to execute the lookup when reading the descriptor file.
   * @see MulePluginModel#getExtensionModelLoaderDescriptor()
   */
  public static final String DESCRIBER_ID = "xml-based";

  @Override
  public String getId() {
    return DESCRIBER_ID;
  }

  @Override
  public ExtensionModel loadExtensionModel(ClassLoader pluginClassLoader, Map<String, Object> attributes) {
    final Object resourceXml = attributes.get(RESOURCE_XML);
    checkNotNull(resourceXml, format("The attribute '%s' is missing", RESOURCE_XML));
    checkArgument(resourceXml instanceof String,
                  format("The attribute '%s' does not have the expected (found '%s', expected '%s')", RESOURCE_XML, resourceXml,
                         String.class.getName()));

    final String modulePath = (String) resourceXml;
    final DescribingContext context = new DefaultDescribingContext(pluginClassLoader);
    final ExtensionFactory defaultExtensionFactory =
        new DefaultExtensionFactory(new SpiServiceRegistry(), pluginClassLoader);
    final XmlBasedDescriber describer = new XmlBasedDescriber(modulePath);
    return withContextClassLoader(pluginClassLoader,
                                  () -> defaultExtensionFactory.createFrom(describer.describe(context), context));
  }
}
