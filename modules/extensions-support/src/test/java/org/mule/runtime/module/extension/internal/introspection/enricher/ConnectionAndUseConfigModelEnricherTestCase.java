/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.checkIsPresent;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.getDeclaration;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.model.property.ConfigTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectivityModelProperty;
import org.mule.test.heisenberg.extension.HeisenbergConnection;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Before;
import org.junit.Test;

public class ConnectionAndUseConfigModelEnricherTestCase
{

    private static final String CALL_SAUL = "callSaul";
    private static final String GET_ENEMY = "getEnemy";
    private ExtensionDeclaration declaration = null;
    private ClassTypeLoader typeLoader;

    @Before
    public void setUp()
    {
        final AnnotationsBasedDescriber basedDescriber = new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver(getProductVersion()));
        ExtensionDeclarer declarer = basedDescriber.describe(new DefaultDescribingContext(getClass().getClassLoader()));
        new ConnectionAndUseConfigModelEnricher().enrich(new DefaultDescribingContext(declarer, this.getClass().getClassLoader()));
        declaration = declarer.getDeclaration();
        typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(HeisenbergExtension.class.getClassLoader());
    }

    @Test
    public void verifyConnectionModelProperty()
    {
        OperationDeclaration operationDeclaration = getDeclaration(declaration.getOperations(), CALL_SAUL);
        final ConnectivityModelProperty connectivityModelProperty = checkIsPresent(operationDeclaration, ConnectivityModelProperty.class);

        assertThat(connectivityModelProperty.getConnectionType(), is(typeLoader.load(HeisenbergConnection.class)));
    }

    @Test
    public void verifyConfigurationModelProperty()
    {
        OperationDeclaration operationDeclaration = getDeclaration(declaration.getOperations(), GET_ENEMY);
        final ConfigTypeModelProperty configTypeModelProperty = checkIsPresent(operationDeclaration, ConfigTypeModelProperty.class);

        assertThat(configTypeModelProperty.getConfigType(), is(typeLoader.load(HeisenbergExtension.class)));
    }
}
