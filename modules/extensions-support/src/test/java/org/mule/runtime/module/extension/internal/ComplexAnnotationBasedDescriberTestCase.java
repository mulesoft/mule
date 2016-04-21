/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;
import static org.mule.test.vegan.extension.VeganExtension.KIWI;
import static org.mule.test.vegan.extension.VeganExtension.VEGAN;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.test.vegan.extension.VeganExtension;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ComplexAnnotationBasedDescriberTestCase extends AbstractAnnotationsBasedDescriberTestCase
{
    private ExtensionDeclaration extensionDeclaration;

    @Before
    public void setUp()
    {
        setDescriber(describerFor(VeganExtension.class));
        extensionDeclaration = describeExtension().getDeclaration();
    }

    @Test
    public void extension()
    {
        assertThat(extensionDeclaration.getName(), is(VEGAN));
        assertThat(extensionDeclaration.getConfigurations(), hasSize(3));
        assertOperation(APPLE, "eatApple");
        assertOperation(BANANA, "eatBanana");
        assertOperation(KIWI, "eatKiwi");
    }

    private void assertOperation(String configName, String operationName)
    {
        ConfigurationDeclaration config = extensionDeclaration.getConfigurations().stream()
                .filter(c -> c.getName().equals(configName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No config with name " + configName));

        assertThat(config.getOperations(), hasSize(1));
        OperationDeclaration operation = config.getOperations().get(0);
        assertThat(operation.getName(), is(operationName));
    }
}
