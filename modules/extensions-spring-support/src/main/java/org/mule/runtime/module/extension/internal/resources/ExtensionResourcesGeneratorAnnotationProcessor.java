/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.capability.xml.description.DescriptionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;


/**
 * {@link BaseExtensionResourcesGeneratorAnnotationProcessor} implementation for default java based extensions.
 *
 * @since 3.7.0
 */
public class ExtensionResourcesGeneratorAnnotationProcessor extends BaseExtensionResourcesGeneratorAnnotationProcessor {

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return new DefaultJavaExtensionModelLoader() {

      @Override
      protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
        super.configureContextBeforeDeclaration(context);
        context.addCustomDeclarationEnricher(new DescriptionDeclarationEnricher());
      }
    };
  }
}
