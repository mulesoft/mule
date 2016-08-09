/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.metadata;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.module.extension.file.api.TreeNode;

/**
 * Resolves the output {@link MetadataType} for an operation that returns an {@link TreeNode}.
 *
 * @since 1.0
 */
public class FileTreeNodeMetadataResolver implements MetadataOutputResolver {

  private FileAttributesMetadataResolver attributesResolver = new FileAttributesMetadataResolver();

  @Override
  public MetadataType getOutputMetadata(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    MetadataType attributes = attributesResolver.getAttributesMetadata(context, key);
    ObjectTypeBuilder<?> treeNode = create(JAVA).objectType();
    treeNode.addField().key("attributes").value(attributes);
    treeNode.addField().key("content").value().anyType();
    treeNode.addField().key("childs").value().arrayType().of(treeNode);

    return treeNode.build();
  }
}
