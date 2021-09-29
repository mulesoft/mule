/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.ClassStereotypeResolver;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class StereotypeModelLoaderDelegate {

  private final Map<StereotypeDefinition, StereotypeModel> stereotypes = new HashMap<>();
  private final String namespace;
  private final Multimap<ComponentDeclaration, ConfigurationDeclaration> componentConfigs = LinkedListMultimap.create();
  private final StereotypeModel sourceParent;
  private final StereotypeModel processorParent;
  private final ClassTypeLoader typeLoader;
  private final DslResolvingContext dslResolvingContext;

  public StereotypeModelLoaderDelegate(ExtensionLoadingContext extensionLoadingContext, String namespace) {
    dslResolvingContext = extensionLoadingContext.getDslResolvingContext();
    ExtensionDeclarer extensionDeclarer = extensionLoadingContext.getExtensionDeclarer();
    this.typeLoader =
        new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionLoadingContext.getExtensionClassLoader());
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    this.namespace = getStereotypePrefix(extensionDeclarer);
    this.processorParent = newStereotype(PROCESSOR.getType(), namespace).withParent(PROCESSOR).build();
    this.sourceParent = newStereotype(SOURCE.getType(), namespace).withParent(SOURCE).build();


    resolveDeclaredTypesStereotypes(declaration, namespace);
  }

  private void resolveStereotype(Type type) {
    new ClassStereotypeResolver(type, declaration, namespace, stereotypes).resolveStereotype();
  }

  private String getStereotypePrefix(ExtensionDeclarer extensionDeclarer) {
    return extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
  }
}
