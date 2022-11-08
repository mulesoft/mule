/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getDeclaration;
import static org.mule.test.module.extension.internal.util.ExtensionDeclarationTestUtils.declarerFor;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.enricher.DsqlDynamicMetadataDeclarationEnricher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.metadata.extension.MetadataExtension;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class DsqlDynamicMetadataDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = declarerFor(MetadataExtension.class, getProductVersion());
    new DsqlDynamicMetadataDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, getClass().getClassLoader(), getDefault(emptySet())));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void outputTypeResolverInformationSetForDsql() throws Exception {
    OperationDeclaration query = getDeclaration(declaration.getOperations(), "doQuery");
    Optional<TypeResolversInformationModelProperty> info = query
        .getModelProperty(TypeResolversInformationModelProperty.class);

    assertThat(info.isPresent(), is(true));
    assertThat(info.get().getCategoryName(), is("QueryResolverCategory"));
    assertThat(info.get().getOutputResolver().get().getResolverName(), is("QUERY_OUTPUT_RESOLVER-ENTITY-NativeQueryResolver"));
  }

}
