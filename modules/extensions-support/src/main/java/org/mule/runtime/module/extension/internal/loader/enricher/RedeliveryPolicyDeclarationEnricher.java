/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.WIRING;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.addRedeliveryPolicy;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

/**
 * A {@link DeclarationEnricher} which adds the following to all {@link SourceDeclaration}:
 * <p>
 * <ul>
 * <li>A Redelivery policy parameter</li>
 * </ul>
 *
 * @since 1.0
 */
public final class RedeliveryPolicyDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return WIRING;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    final Reference<Boolean> hasObjectStoreParams = new Reference<>(false);

    ExtensionDeclaration extension = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    new IdempotentDeclarationWalker() {

      @Override
      protected void onSource(SourceDeclaration declaration) {
        addRedeliveryPolicy(declaration);
        hasObjectStoreParams.set(true);
      }
    }.walk(extension);

    if (hasObjectStoreParams.get() && !isObjectStoreAlreadyImported(extension)) {
      ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
      extension.getImportedTypes().add(new ImportedTypeModel((ObjectType) typeLoader.load(ObjectStore.class)));
    }
  }

  private boolean isObjectStoreAlreadyImported(ExtensionDeclaration extension) {
    return extension.getImportedTypes().stream().anyMatch(model -> isObjectStore(model.getImportedType()));
  }

  private boolean isObjectStore(MetadataType type) {
    return getTypeId(type)
        .filter(typeId -> ObjectStore.class.getName().equals(typeId))
        .isPresent();
  }
}
