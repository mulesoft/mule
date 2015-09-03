/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.reflections.ReflectionUtils.getMethods;
import org.mule.extension.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.introspection.declaration.fluent.OperationDescriptor;
import org.mule.module.extension.spi.CapabilityExtractor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractCapabilitiesExtractorContractTestCase extends AbstractMuleTestCase
{

    protected CapabilityExtractor capabilityExtractor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected DeclarationDescriptor declarationDescriptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected ConfigurationDescriptor configurationDescriptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected OperationDescriptor operationDescriptor;

    @Before
    public void before()
    {
        capabilityExtractor = createCapabilityExtractor();
    }

    protected abstract CapabilityExtractor createCapabilityExtractor();

    @Test
    public void extractExtensionCapability()
    {
        assertThat(capabilityExtractor.extractExtensionCapability(declarationDescriptor, getClass()), is(nullValue()));
    }

    @Test
    public void extractConfigCapability()
    {
        assertThat(capabilityExtractor.extractConfigCapability(configurationDescriptor, getClass()), is(nullValue()));
    }

    @Test
    public void extractOperationCapability()
    {
        assertThat(capabilityExtractor.extractOperationCapability(operationDescriptor, getMethods(getClass(), input -> true).iterator().next()), is(nullValue()));
    }

}
