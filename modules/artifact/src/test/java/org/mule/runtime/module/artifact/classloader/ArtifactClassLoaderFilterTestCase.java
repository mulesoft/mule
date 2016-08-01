/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.annotation.Annotation;
import java.util.Collections;

import org.junit.Test;

@SmallTest
public class ArtifactClassLoaderFilterTestCase extends AbstractMuleTestCase
{

    private ArtifactClassLoaderFilter filter = new ArtifactClassLoaderFilter(Collections.singleton("java.lang"), Collections.singleton("META-INF"));

    @Test
    public void filtersClassWhenPackageNotExported() throws Exception
    {
        assertThat(filter.exportsClass(java.io.Closeable.class.getName()), equalTo(false));
    }

    @Test
    public void filtersClassWhenPackageNotExportedAndParentPackageIsExported() throws Exception
    {
        assertThat(filter.exportsClass(Annotation.class.getName()), equalTo(false));
    }

    @Test
    public void acceptsClassWhenPackageExported() throws Exception
    {
        assertThat(filter.exportsClass(Object.class.getName()), equalTo(true));
    }

    @Test
    public void acceptsResourceWhenPackageExported() throws Exception
    {
        assertThat(filter.exportsResource("/META-INF/schema.xsd"), equalTo(true));
    }

    @Test
    public void filtersResourceWhenPackageNotExported() throws Exception
    {
        assertThat(filter.exportsResource("/DOC/readme.txt"), equalTo(false));
    }

    @Test
    public void filtersResourceWhenPackageNotExportedAndParentPackageIsExported() throws Exception
    {
        assertThat(filter.exportsResource("/META-INF/XML/sample.xml"), equalTo(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validatesNullClassName() throws Exception
    {
        filter.exportsClass(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validatesEmptyClassName() throws Exception
    {
        filter.exportsClass("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void validatesNullResourceName() throws Exception
    {
        filter.exportsResource(null);
    }

    @Test
    public void validatesEmptyResourceName() throws Exception
    {
        assertThat(filter.exportsResource(""), equalTo(false));
    }
}
