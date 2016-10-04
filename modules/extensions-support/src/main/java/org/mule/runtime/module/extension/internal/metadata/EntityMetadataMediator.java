/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.typeDescriptor;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataKeysContainerBuilder;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;

import java.util.Set;

/**
 * Resolves a Component's Metadata by coordinating the several moving parts that are affected by the Metadata fetching process, so
 * that such pieces can remain decoupled.
 * <p/>
 * This mediator will coordinate the resolvers: {@link MetadataResolverFactory}, {@link TypeKeysResolver},
 * {@link InputTypeResolver} and {@link OutputTypeResolver}, and the descriptors that represent their results:
 * {@link ComponentMetadataDescriptor}, {@link OutputMetadataDescriptor} and {@link TypeMetadataDescriptor}
 *
 * @since 4.0
 */
public class EntityMetadataMediator {

  private final MetadataResolverFactory resolverFactory;

  public EntityMetadataMediator(RuntimeOperationModel operationModel) {
    this.resolverFactory = operationModel.getMetadataResolverFactory();
  }

  public MetadataResult<MetadataKeysContainer> getEntityKeys(MetadataContext context) {
    try {
      QueryEntityResolver queryEntityResolver = resolverFactory.getQueryEntityResolver();
      Set<MetadataKey> entityKeys = queryEntityResolver.getEntityKeys(context);
      final MetadataKeysContainerBuilder keyBuilder = MetadataKeysContainerBuilder.getInstance();
      if (entityKeys.stream().anyMatch(key -> key.getChilds().size() > 0)) {
        throw new IllegalArgumentException("Entity keys must not contain childs. "
            + "Only single level keys are supported for Entity Metadata Retrieval");
      }
      return success(keyBuilder.add(queryEntityResolver.getClass().getSimpleName(), entityKeys).build());
    } catch (Exception e) {
      return failure(e);
    }
  }

  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(MetadataContext context, MetadataKey entityKey) {
    try {
      MetadataType entityMetadata = resolverFactory.getQueryEntityResolver().getEntityMetadata(context, entityKey.getId());
      return success(typeDescriptor().withType(entityMetadata).build());
    } catch (Exception e) {
      return failure(e);
    }
  }

}
