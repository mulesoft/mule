/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.resources;

import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.extensions.resources.GenerableResource;
import org.mule.extensions.resources.ResourcesGenerator;
import org.mule.tck.size.SmallTest;

import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AnnotationProcessorResourceGeneratorTestCase extends ResourcesGeneratorContractTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProcessingEnvironment processingEnvironment;

    @Override
    protected ResourcesGenerator buildGenerator()
    {
        return new AnnotationProcessorResourceGenerator(processingEnvironment, serviceRegistry);
    }

    @Test
    public void dumpAll() throws Exception
    {
        final String filepath = "path";
        final String content = "hello world!";

        GenerableResource resource = generator.getOrCreateResource(filepath);
        resource.getContentBuilder().append(content);

        FileObject file = mock(FileObject.class);
        when(processingEnvironment.getFiler().createResource(SOURCE_OUTPUT, EMPTY, filepath)).thenReturn(file);

        OutputStream out = mock(OutputStream.class, RETURNS_DEEP_STUBS);
        when(file.openOutputStream()).thenReturn(out);

        generator.dumpAll();

        verify(out).write(content.getBytes());
        verify(out).flush();
    }
}
