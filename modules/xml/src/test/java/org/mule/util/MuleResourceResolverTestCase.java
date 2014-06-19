/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.module.xml.util.MuleResourceResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.w3c.dom.ls.LSInput;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

@SmallTest
public class MuleResourceResolverTestCase extends AbstractMuleTestCase
{
    private static final String NON_EXISTENT_RESOURCE = "non-existent-resource";

    private static final String EXISTENT_CLASSPATH_SUBDIRECTORY = "localresourceresolver";

    private static final String EXISTENT_CLASSPATH_SUBDIRECTORY_RESOURCE = "resource.txt";

    private static final String EXISTENT_CLASSPATH_ABSOLUTE_RESOURCE = EXISTENT_CLASSPATH_SUBDIRECTORY + "/" + EXISTENT_CLASSPATH_SUBDIRECTORY_RESOURCE;

    private static final String IDENTIFIER_TYPE = "myTestType";

    private static final String IDENTIFIER_NAMESPACE = "myTestNameSpace";

    private static final String IDENTIFIER_PUBLIC_ID = "myTestPublicId";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    MuleResourceResolver resolver;

    @Before
    public void prepare()
    {
        resolver = new MuleResourceResolver();
    }

    @Test
    public void testsNonExistentResource()
    {
        LSInput outcome = resolver.resolveResource(IDENTIFIER_TYPE, IDENTIFIER_NAMESPACE, IDENTIFIER_PUBLIC_ID, NON_EXISTENT_RESOURCE, null);
        assertThat(outcome, is(nullValue()));
    }

    @Test
    public void testsExistentClasspathResource()
    {
        LSInput outcome = resolver.resolveResource(IDENTIFIER_TYPE, IDENTIFIER_NAMESPACE, IDENTIFIER_PUBLIC_ID, EXISTENT_CLASSPATH_ABSOLUTE_RESOURCE, null);
        assertThat(outcome, is(not(nullValue())));
        assertThat(outcome.getBaseURI(), is(nullValue()));
        assertCommon(outcome);
    }

    @Test
    public void testsExistentAbsoluteFileSystemResource() throws IOException
    {
        File file = temporaryFolder.newFile(UUID.getUUID());
        LSInput outcome = resolver.resolveResource(IDENTIFIER_TYPE, IDENTIFIER_NAMESPACE, IDENTIFIER_PUBLIC_ID, file.getAbsolutePath(), null);
        assertThat(outcome, is(not(nullValue())));
        assertThat(outcome.getBaseURI(), is(nullValue()));
        assertCommon(outcome);
    }

    @Test
    public void testsNonExistentAbsoluteFileSystemResource() throws IOException
    {
        String nonExistentAbsoluteFileSystemResource = new File(temporaryFolder.getRoot(), NON_EXISTENT_RESOURCE).getAbsolutePath();
        LSInput outcome = resolver.resolveResource(IDENTIFIER_TYPE, IDENTIFIER_NAMESPACE, IDENTIFIER_PUBLIC_ID, nonExistentAbsoluteFileSystemResource, null);
        assertThat(outcome, is(nullValue()));
    }

    @Test
    public void testsExistentFileSystemResourceWithBaseUri() throws IOException
    {
        File file = temporaryFolder.newFile(UUID.getUUID());
        LSInput outcome = resolver.resolveResource(IDENTIFIER_TYPE, IDENTIFIER_NAMESPACE, IDENTIFIER_PUBLIC_ID, file.getName(), file.getPath());
        assertThat(outcome, is(not(nullValue())));
        assertThat(outcome.getBaseURI(), is(not(nullValue())));
        assertCommon(outcome);
    }

    @Test
    public void testsNonExistentFileSystemResourceWithBaseUri() throws IOException
    {
        File file = temporaryFolder.newFile(UUID.getUUID());
        LSInput outcome = resolver.resolveResource(IDENTIFIER_TYPE, IDENTIFIER_NAMESPACE, IDENTIFIER_PUBLIC_ID, file.getName(), NON_EXISTENT_RESOURCE);
        assertThat(outcome, is(nullValue()));
    }

    private void assertCommon(LSInput outcome)
    {
        assertThat(outcome.getByteStream(), is(not(nullValue())));
        assertThat(outcome.getSystemId(), is(not(nullValue())));
        assertThat(outcome.getPublicId(), is(IDENTIFIER_PUBLIC_ID));
    }

}
