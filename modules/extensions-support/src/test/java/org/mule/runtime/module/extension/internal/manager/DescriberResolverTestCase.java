/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DESCRIBER_ID;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DescriberResolverTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ExtensionManifest manifest;

    private final DescriberResolver resolver = new DescriberResolver();
    private final ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());

    @Before
    public void before()
    {
        when(manifest.getName()).thenReturn(HEISENBERG);
        when(manifest.getVersion()).thenReturn("4.0.0");
    }

    @Test
    public void getDescriber() throws Exception
    {
        when(manifest.getDescriberManifest().getId()).thenReturn(DESCRIBER_ID);
        when(manifest.getDescriberManifest().getProperties()).thenReturn(ImmutableMap.<String, String>builder()
                                                                                 .put(AnnotationsBasedDescriber.TYPE_PROPERTY_NAME, HeisenbergExtension.class.getName())
                                                                                 .build());

        Describer describer = resolver.resolve(manifest, getClass().getClassLoader());
        assertThat(describer, instanceOf(AnnotationsBasedDescriber.class));

        final DefaultDescribingContext context = new DefaultDescribingContext();
        ExtensionModel model = extensionFactory.createFrom(describer.describe(context), context);
        assertThat(model.getName(), equalTo(manifest.getName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedDescriber()
    {
        when(manifest.getDescriberManifest().getId()).thenReturn("funkyDescriber");
        resolver.resolve(manifest, getClass().getClassLoader());
    }

}
