/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;

import java.util.List;

/**
 * Enriches all parameters of type {@link ObjectStore} adding the {@link MuleStereotypes#OBJECT_STORE} stereotype if not already
 * present. If at least one parameter of that type is found, it also makes sure that {@link ObjectStore} is registered as an
 * imported type
 *
 * @since 4.1
 */
public class JavaObjectStoreParameterDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    final ExtensionDeclaration extension = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    final Reference<Boolean> hasObjectStoreParams = new Reference<>(false);

    new IdempotentDeclarationWalker() {

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration parameter) {
        if (isObjectStore(parameter.getType())) {
          List<StereotypeModel> stereotypes = parameter.getAllowedStereotypeModels();
          if (!stereotypes.contains(OBJECT_STORE)) {
            stereotypes.add(OBJECT_STORE);
          }
          hasObjectStoreParams.set(true);
        }
      }
    }.walk(extension);


    if (hasObjectStoreParams.get() && !isObjectStoreAlreadyImported(extension)) {
      ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
      extension.getImportedTypes().add(new ImportedTypeModel((ObjectType) typeLoader.load(ObjectStore.class)));
    }
  }

  private boolean isObjectStoreAlreadyImported(ExtensionDeclaration extension) {
    return extension.getImportedTypes().stream().filter(model -> isObjectStore(model.getImportedType())).findAny().isPresent();
  }

  private boolean isObjectStore(MetadataType type) {
    return getTypeId(type)
        .filter(typeId -> ObjectStore.class.getName().equals(typeId))
        .isPresent();
  }
}
