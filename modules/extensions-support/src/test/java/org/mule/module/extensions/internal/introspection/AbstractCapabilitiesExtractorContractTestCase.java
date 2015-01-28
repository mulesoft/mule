/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mule.api.registry.SPIServiceRegistry;
import org.mule.extensions.introspection.declaration.Construct;
import org.mule.extensions.introspection.declaration.DeclarationConstruct;
import org.mule.extensions.introspection.declaration.HasCapabilities;
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

    @Mock
    protected HasCapabilities<Construct> capabilitiesCallback;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected DeclarationConstruct declarationConstruct;

    @Before
    public void before()
    {
        resolver = new DefaultCapabilitiesResolver(new SPIServiceRegistry());
    }

    @Test
    public void noCapability()
    {
        resolver.resolveCapabilities(declarationConstruct, getClass(), capabilitiesCallback);
        verify(capabilitiesCallback, never()).withCapability(anyObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullClass()
    {
        resolver.resolveCapabilities(declarationConstruct, null, capabilitiesCallback);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCallback()
    {
        resolver.resolveCapabilities(declarationConstruct, getClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDeclaration()
    {
        resolver.resolveCapabilities(null, getClass(), capabilitiesCallback);
    }

}
