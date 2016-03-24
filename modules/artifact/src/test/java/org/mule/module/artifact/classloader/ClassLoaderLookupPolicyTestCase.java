/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import org.junit.Test;

@SmallTest
public class ClassLoaderLookupPolicyTestCase extends AbstractMuleTestCase
{

    @Test
    public void detectsBlockedClass() throws Exception
    {
        final ClassLoaderLookupPolicy lookupPolicy = new ClassLoaderLookupPolicy(Collections.emptySet(), Collections.singleton("org.foo"));
        assertThat(lookupPolicy.isBlocked("org.foo.MyClass"), is(true));
        assertThat(lookupPolicy.isBlocked("org.foo.bar.MyClassFactory"), is(false));
        assertThat(lookupPolicy.isBlocked("org.MyInterface"), is(false));
    }

    @Test
    public void detectsOverriddenClass() throws Exception
    {
        final ClassLoaderLookupPolicy lookupPolicy = new ClassLoaderLookupPolicy(Collections.singleton("org.foo"), Collections.emptySet());
        assertThat(lookupPolicy.isOverridden("org.foo.MyClass"), is(true));
        assertThat(lookupPolicy.isOverridden("org.foo.bar.MyClassFactory"), is(false));
        assertThat(lookupPolicy.isOverridden("org.MyInterface"), is(false));
    }
}