/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

@SmallTest
public class ArtifactClassLoaderFilterTestCase extends AbstractMuleTestCase
{

    public static final String CLASS_NAME = "java.lang.Object";
    public static final Set<String> CLASS_NAMES = Collections.singleton(CLASS_NAME);
    public static final String CLASS_PREFIX = "java.lang";
    public static final Set<String> PREFIX_NAMES = Collections.singleton(CLASS_PREFIX);

    private final ArtifactDescriptor descriptor = new ArtifactDescriptor();
    private ArtifactClassLoaderFilter filter = new ArtifactClassLoaderFilter(descriptor);

    @Test
    public void filtersClassWhenClassAndPrefixAreNotExported() throws Exception
    {
        assertThat(filter.accepts(CLASS_NAME), equalTo(false));
    }

    @Test
    public void acceptsClassWhenClassExported() throws Exception
    {
        descriptor.setExportedPrefixNames(CLASS_NAMES);
        assertThat(filter.accepts(CLASS_NAME), equalTo(true));
    }

    @Test
    public void acceptsClassWhenPrefixExported() throws Exception
    {
        descriptor.setExportedPrefixNames(PREFIX_NAMES);
        assertThat(filter.accepts(CLASS_NAME), equalTo(true));
    }

    @Test
    public void filtersClassWhenClassBlocked() throws Exception
    {
        descriptor.setExportedPrefixNames(CLASS_NAMES);
        descriptor.setBlockedPrefixNames(CLASS_NAMES);
        assertThat(filter.accepts(CLASS_NAME), equalTo(false));
    }

    @Test
    public void filtersClassWhenPrefixBlocked() throws Exception
    {
        descriptor.setExportedPrefixNames(PREFIX_NAMES);
        descriptor.setBlockedPrefixNames(PREFIX_NAMES);
        assertThat(filter.accepts(CLASS_NAME), equalTo(false));
    }

    @Test
    public void acceptsClassWhenClassExportedAndPrefixBlocked() throws Exception
    {
        descriptor.setExportedPrefixNames(CLASS_NAMES);
        descriptor.setBlockedPrefixNames(PREFIX_NAMES);
        assertThat(filter.accepts(CLASS_NAME), equalTo(true));
    }
}
