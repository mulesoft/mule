/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.module.extensions.internal.introspection.ExtensionDiscoverer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultExtensionsManagerTestCase extends AbstractMuleTestCase
{

    private DefaultExtensionsManager extensionsManager;

    private static final String EXTENSION1_NAME = "extension1";
    private static final String EXTENSION2_NAME = "extension2";
    private static final String EXTENSION1_VERSION = "3.6.0";
    private static final String EXTENSION2_VERSION = "3.6.0";
    private static final String NEWER_VERSION = "4.0";
    private static final String OLDER_VERSION = "3.5.1";

    @Mock
    private ExtensionDiscoverer discoverer;

    @Mock
    private Extension extension1;

    @Mock
    private Extension extension2;

    private ClassLoader classLoader;

    @Before
    public void before()
    {
        extensionsManager = new DefaultExtensionsManager();
        extensionsManager.setExtensionsDiscoverer(discoverer);

        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension2.getName()).thenReturn(EXTENSION2_NAME);

        when(extension1.getVersion()).thenReturn(EXTENSION1_VERSION);
        when(extension2.getVersion()).thenReturn(EXTENSION2_VERSION);

        classLoader = getClass().getClassLoader();

        when(discoverer.discover(same(classLoader))).thenAnswer(new Answer<List<Extension>>()
        {
            @Override
            public List<Extension> answer(InvocationOnMock invocation) throws Throwable
            {
                return getTestExtensions();
            }
        });
    }

    @Test
    public void discover()
    {
        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        verify(discoverer).discover(same(classLoader));
        testEquals(getTestExtensions(), extensions);
    }

    @Test
    public void getExtensions()
    {
        discover();
        testEquals(getTestExtensions(), extensionsManager.getExtensions());
    }

    @Test
    public void getExtensionsCapableOf()
    {
        when(extension1.isCapableOf(XmlCapability.class)).thenReturn(true);
        when(extension2.isCapableOf(XmlCapability.class)).thenReturn(false);

        discover();
        Set<Extension> extensions = extensionsManager.getExtensionsCapableOf(XmlCapability.class);

        assertThat(extensions, hasSize(1));
        testEquals(extension1, extensions.iterator().next());
    }

    @Test
    public void noExtensionsCapableOf()
    {
        when(extension1.isCapableOf(XmlCapability.class)).thenReturn(false);
        when(extension2.isCapableOf(XmlCapability.class)).thenReturn(false);

        discover();
        Set<Extension> extensions = extensionsManager.getExtensionsCapableOf(XmlCapability.class);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void extensionsCapableOfNull()
    {
        extensionsManager.getExtensionsCapableOf(null);
    }

    @Test
    public void hotUpdateNewerExtension()
    {
        discover();

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn(NEWER_VERSION);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions, hasSize(1));
        testEquals(extension1, extensions.get(0));
    }

    @Test
    public void hotUpdateOlderVersion()
    {
        discover();

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn(OLDER_VERSION);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test
    public void hotUpdateWithNoChanges()
    {
        discover();
        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test
    public void hotUpdateWithInvalidVersion()
    {
        discover();

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn("brandnew");

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test
    public void hotUpdateWithOneInvalidVersion()
    {
        discover();

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn("brandnew");

        extension2 = mock(Extension.class);
        when(extension2.getName()).thenReturn(EXTENSION2_NAME);
        when(extension2.getVersion()).thenReturn(NEWER_VERSION);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions, hasSize(1));
        testEquals(extension2, extensions.get(0));
    }

    @Test
    public void contextClassLoaderKept()
    {
        discover();
        assertThat(classLoader, sameInstance(Thread.currentThread().getContextClassLoader()));
    }

    @Test
    public void contextClassLoaderKeptAfterException()
    {
        when(discoverer.discover(same(classLoader))).thenThrow(RuntimeException.class);
        try
        {
            discover();
            fail("was expecting an exception");
        }
        catch (RuntimeException e)
        {
            assertThat(classLoader, sameInstance(Thread.currentThread().getContextClassLoader()));
        }
    }

    private List<Extension> getTestExtensions()
    {
        return ImmutableList.<Extension>builder()
                .add(extension1)
                .add(extension2)
                .build();
    }

    private void testEquals(Collection<Extension> expected, Collection<Extension> obtained)
    {
        assertThat(obtained.size(), is(expected.size()));
        Iterator<Extension> expectedIterator = expected.iterator();
        Iterator<Extension> obtainedIterator = expected.iterator();

        while (expectedIterator.hasNext())
        {
            assertThat(obtainedIterator.hasNext(), is(true));
            testEquals(expectedIterator.next(), obtainedIterator.next());
        }
    }

    private void testEquals(Extension expected, Extension obtained)
    {
        assertThat(obtained.getName(), equalTo(expected.getName()));
        assertThat(obtained.getVersion(), equalTo(expected.getVersion()));
    }

}
