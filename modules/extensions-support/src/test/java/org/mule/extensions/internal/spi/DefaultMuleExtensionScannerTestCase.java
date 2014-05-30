/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal.spi;

import static junit.framework.Assert.assertEquals;
import org.mule.extensions.HeisenbergModule;
import org.mule.extensions.api.MuleExtensionsManager;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.spi.MuleExtensionScanner;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultMuleExtensionScannerTestCase extends AbstractMuleTestCase
{

    @Mock
    private MuleExtensionsManager extensionsManager;

    private MuleExtensionScanner scanner;

    @Before
    public void setUp()
    {
        scanner = new DefaultMuleExtensionScanner();
    }

    @Test
    public void scan() throws Exception
    {
        List<MuleExtension> extensions = scanner.scan();
        assertEquals(1, extensions.size());

        MuleExtension extension = extensions.get(0);
        assertMuleExtension(extension);

    }

    @Test
    public void scanAndRegister() throws Exception
    {
        scanner.scanAndRegister(extensionsManager);
        ArgumentCaptor<MuleExtension> captor = ArgumentCaptor.forClass(MuleExtension.class);
        Mockito.verify(extensionsManager).register(captor.capture());

        MuleExtension extension = captor.getValue();
        assertMuleExtension(extension);
    }

    private void assertMuleExtension(MuleExtension extension)
    {
        assertEquals(HeisenbergModule.EXTENSION_NAME, extension.getName());
    }
}
