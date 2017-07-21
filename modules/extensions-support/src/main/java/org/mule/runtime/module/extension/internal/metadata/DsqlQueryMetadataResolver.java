/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.extension.api.dsql.DsqlQuery;
import org.mule.runtime.extension.api.dsql.Field;

import java.util.List;

/**
 * {@link OutputTypeResolver} implementation that automatic resolves the output {@link MetadataType} for a given
 * {@link DsqlQuery}.
 * <p>
 * This resolver goes for all the selected fields in the {@link DsqlQuery} and return a new entity with a subset of the total
 * field of the entity, unless all the fields were selected ("*") that the whole entity {@link MetadataType} is returned.
 *
 * @since 4.0
 */
final class DsqlQueryMetadataResolver implements OutputTypeResolver {

  private final QueryEntityResolver entityResolver;
  private final OutputTypeResolver nativeOutputResolver;

  DsqlQueryMetadataResolver(QueryEntityResolver entityResolver,
                            OutputTypeResolver nativeOutputResolver) {
    this.entityResolver = entityResolver;
    this.nativeOutputResolver = nativeOutputResolver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCategoryName() {
    return nativeOutputResolver.getCategoryName();
  }

  /**
   * Automatically resolves the output metadata for the {@link DsqlQuery}.
   * <p>
   * The base entity is resolved using the component {@link QueryEntityResolver} and assuming the key of the entity is the DSQL
   * {@link DsqlQuery#getType() type}.
   *
   * @param context {@link MetadataContext} of the MetaData resolution
   * @param query the {@link DsqlQuery} to resolve the output metadata from.
   */
  @Override
  public MetadataType getOutputType(MetadataContext context, Object query)
      throws MetadataResolvingException, ConnectionException {

    if (query instanceof DsqlQuery) {

      DsqlQuery dsqlQuery = (DsqlQuery) query;
      MetadataType entityMetadata = entityResolver.getEntityMetadata(context, dsqlQuery.getType().getName());

      BaseTypeBuilder builder = context.getTypeBuilder();
      final List<Field> fields = dsqlQuery.getFields();
      if (fields.size() == 1 && fields.get(0).getName().equals("*")) {
        return entityMetadata;
      }

      entityMetadata.accept(new MetadataTypeVisitor() {

        @Override
        public void visitObject(ObjectType objectType) {
          ObjectTypeBuilder objectTypeBuilder = builder.objectType();
          objectType.getFields()
              .stream()
              .filter(p -> fields.stream().anyMatch(f -> f.getName().equalsIgnoreCase(p.getKey().getName().getLocalPart())))
              .forEach(p -> {
                ObjectFieldTypeBuilder field = objectTypeBuilder.addField();
                field.key(p.getKey().getName());
                field.value(p.getValue());
              });
        }
      });

      return builder.build();
    } else {
      return nativeOutputResolver.getOutputType(context, query);
    }
  }
}
