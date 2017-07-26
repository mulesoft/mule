/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.extension.api.dsql.DsqlParser;
import org.mule.runtime.extension.api.dsql.DsqlQuery;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.tck.size.SmallTest;
import org.mule.test.metadata.extension.query.MetadataExtensionEntityResolver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DsqlQueryMetadataResolverTestCase {

  private static final DsqlParser dsqlParser = DsqlParser.getInstance();

  @Mock
  private MetadataContext context;

  @Before
  public void setUp() {
    when(context.getTypeBuilder()).thenReturn(new BaseTypeBuilder(MetadataFormat.JAVA));
  }

  @Test
  public void getTrimmedOutputMetadata() throws MetadataResolvingException, ConnectionException {
    DsqlQuery dsqlQuery = dsqlParser.parse("dsql:SELECT id FROM Circle WHERE (diameter < 18)");
    MetadataType outputMetadata = getQueryMetadataResolver().getOutputType(context, dsqlQuery);

    ObjectType type = getAndAssertTypeOf(outputMetadata);
    assertThat(type.getFields(), hasSize(1));
    ObjectFieldType onlyField = type.getFields().iterator().next();
    assertThat(onlyField.getValue(), is(instanceOf(NumberType.class)));
    assertThat(onlyField.getKey().getName().getLocalPart(), is("id"));
  }

  @Test
  public void getFullOutputMetadata() throws MetadataResolvingException, ConnectionException {
    DsqlQuery dsqlQuery = dsqlParser.parse("dsql:SELECT * FROM Circle WHERE (diameter < 18)");
    MetadataType outputMetadata = getQueryMetadataResolver().getOutputType(context, dsqlQuery);

    ObjectType type = getAndAssertTypeOf(outputMetadata);
    assertThat(type.getFields(), hasSize(3));

    type.getFields().forEach(f -> {
      String name = f.getKey().getName().getLocalPart();
      assertThat(name, isIn(asList("color", "id", "diameter")));
    });
  }

  private ObjectType getAndAssertTypeOf(MetadataType outputMetadata) {
    assertThat(outputMetadata, is(instanceOf(ObjectType.class)));
    return (ObjectType) outputMetadata;
  }

  private DsqlQueryMetadataResolver getQueryMetadataResolver() {
    NullMetadataResolver outputResolver = new NullMetadataResolver();
    MetadataExtensionEntityResolver entityResolver = new MetadataExtensionEntityResolver();
    return new DsqlQueryMetadataResolver(entityResolver, outputResolver);
  }
}
