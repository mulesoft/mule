/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.registry.SpiServiceRegistry;
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
    protected CapabilitiesResolver resolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected DeclarationDescriptor declarationDescriptor;

    @Before
    public void before()
    {
        resolver = new DefaultCapabilitiesResolver(new SpiServiceRegistry());
    }

    @Test
    public void noCapability()
    {
        resolver.resolveCapabilities(declarationDescriptor, getClass());
        verify(declarationDescriptor, never()).withCapability(anyObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullClass()
    {
        resolver.resolveCapabilities(declarationDescriptor, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDeclaration()
    {
        resolver.resolveCapabilities(null, getClass());
    }

}
