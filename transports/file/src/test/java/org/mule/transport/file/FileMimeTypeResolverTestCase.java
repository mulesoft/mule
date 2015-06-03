/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mule.transport.file.FileMimeTypeResolver.mimeTypes;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class FileMimeTypeResolverTestCase extends AbstractMuleTestCase
{

    @Test
    public void resolvesFileMimeType() throws Exception
    {
        for (String extension : mimeTypes.keySet())
        {
            doFileMimeTypeTest(extension, mimeTypes.get(extension));
        }
    }

    @Test
    public void resolvesUpperCaseFileMimeType() throws Exception
    {
        for (String extension : mimeTypes.keySet())
        {
            doFileMimeTypeTest(extension.toUpperCase(), mimeTypes.get(extension));
        }
    }

    @Test
    public void returnsNullOnNotResolvedMimeType() throws Exception
    {
        doFileMimeTypeTest("xxx", null);
    }

    private void doFileMimeTypeTest(String fileExtension, String expectedMimeType)
    {
        final String fileName = "test." + fileExtension;

        final String mimeType = FileMimeTypeResolver.resolveFileMimeType(fileName);

        assertThat(mimeType, equalTo(expectedMimeType));
    }
}