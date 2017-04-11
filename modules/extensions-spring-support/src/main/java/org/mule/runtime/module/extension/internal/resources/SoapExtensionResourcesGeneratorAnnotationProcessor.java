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
import org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader;


/**
 * {@link BaseExtensionResourcesGeneratorAnnotationProcessor} implementation for SOAP BASED Extensions that use the
 * soap extensions api classes and annotations.
 *
 * @since 4.0.0
 */
public class SoapExtensionResourcesGeneratorAnnotationProcessor extends BaseExtensionResourcesGeneratorAnnotationProcessor {

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return new SoapExtensionModelLoader() {

      @Override
      protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
        super.configureContextBeforeDeclaration(context);
        context.addCustomDeclarationEnricher(new DescriptionDeclarationEnricher());
      }
    };
  }
}
