/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.test.metadata.extension.query.MetadataExtensionEntityResolver.CIRCLE;
import static org.mule.test.metadata.extension.query.MetadataExtensionEntityResolver.SQUARE;
import static org.mule.test.metadata.extension.query.NativeQueryOutputResolver.NATIVE_QUERY;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.metadata.extension.model.animals.Bear;
import org.mule.test.metadata.extension.model.shapes.Circle;

import java.util.Set;

import org.junit.Test;

public class QueryMetadataTestCase extends MetadataExtensionFunctionalTestCase {

  private static final MetadataKey CIRCLE_METADATA_KEY = newKey(CIRCLE).build();
  private static final ProcessorId QUERY_ID = new ProcessorId(QUERY_FLOW, FIRST_PROCESSOR_INDEX);;

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getEntityKeys() throws Exception {
    MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataManager.getEntityKeys(QUERY_ID);
    assertThat(metadataKeysResult.isSuccess(), is(true));
    MetadataKeysContainer container = metadataKeysResult.get();
    Set<MetadataKey> metadataKeys = container.getKeys(container.getResolvers().iterator().next()).get();
    assertThat(metadataKeys.size(), is(2));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(CIRCLE), metadataKeyWithId(SQUARE)));
  }

  @Test
  public void getEntityMetadata() throws Exception {
    MetadataResult<TypeMetadataDescriptor> entityMetadata = metadataManager.getEntityMetadata(QUERY_ID, CIRCLE_METADATA_KEY);
    assertThat(entityMetadata.isSuccess(), is(true));
    TypeMetadataDescriptor descriptor = entityMetadata.get();
    assertThat(descriptor.getType(), is(toMetadataType(Circle.class)));
  }

  @Test
  public void getDsqlQueryAutomaticGeneratedOutputMetadata() throws Exception {
    MetadataKey dsqlKey = newKey(DSQL_QUERY).build();
    MetadataResult<ComponentMetadataDescriptor> entityMetadata = metadataManager.getMetadata(QUERY_ID, dsqlKey);
    assertThat(entityMetadata.isSuccess(), is(true));

    TypeMetadataDescriptor descriptor = entityMetadata.get().getOutputMetadata().get().getPayloadMetadata().get();
    MetadataType generatedType = descriptor.getType();
    assertThat(generatedType, is(instanceOf(ArrayType.class)));

    ObjectType fields = (ObjectType) ((ArrayType) generatedType).getType();
    assertThat(fields.getFields(), hasSize(1));
    ObjectFieldType field = fields.getFields().iterator().next();
    assertThat(field.getKey().getName().getLocalPart(), is("id"));
    assertThat(field.getValue(), is(instanceOf(NumberType.class)));
  }

  @Test
  public void getNativeQueryOutputMetadata() throws Exception {
    MetadataKey nativeKey = newKey(NATIVE_QUERY).build();
    MetadataResult<ComponentMetadataDescriptor> entityMetadata = metadataManager.getMetadata(QUERY_ID, nativeKey);

    assertThat(entityMetadata.isSuccess(), is(true));
    TypeMetadataDescriptor output = entityMetadata.get().getOutputMetadata().get().getPayloadMetadata().get();
    assertThat(output.getType(), is(toMetadataType(Bear.class)));
  }

}
