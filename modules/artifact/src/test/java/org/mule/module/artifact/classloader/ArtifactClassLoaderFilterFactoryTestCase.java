/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.artifact.classloader.ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.junit.Test;

public class ArtifactClassLoaderFilterFactoryTestCase extends AbstractMuleTestCase
{

    private final ArtifactClassLoaderFilterFactory factory = new ArtifactClassLoaderFilterFactory();

    @Test
    public void createsNullFilter() throws Exception
    {
        assertThat(factory.create(null, null), is(NULL_CLASSLOADER_FILTER));
        assertThat(factory.create("", null), is(NULL_CLASSLOADER_FILTER));
        assertThat(factory.create(null, ""), is(NULL_CLASSLOADER_FILTER));
    }

    @Test
    public void createsFilter() throws Exception
    {
        final ClassLoaderFilter filter = factory.create("java.lang, java.lang.annotation", "/META-INF, META-INF/xml");

        assertThat(filter.exportsClass(Object.class.getName()), is(true));
        assertThat(filter.exportsClass(Annotation.class.getName()), is(true));
        assertThat(filter.exportsClass(AnnotatedElement.class.getName()), is(false));

        assertThat(filter.exportsResource("META-INF/MANIFEST.MF"), is(true));
        assertThat(filter.exportsResource("META-INF/xml/schema.xsd"), is(true));
        assertThat(filter.exportsResource("META-INF/doc/readme.txt"), is(false));
    }
}