/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.resources;


import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.apache.commons.lang.StringUtils.EMPTY;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extensions.resources.GenerableResource;
import org.mule.extensions.resources.ResourcesGenerator;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;

/**
 * Implementation of {@link ResourcesGenerator}
 * that writes files using a {@link javax.annotation.processing.Filer} obtained
 * through a annotation {@link javax.annotation.processing.Processor} context
 *
 * @since 3.7.0
 */
final class AnnotationProcessorResourceGenerator extends AbstractResourcesGenerator
{

    private final ProcessingEnvironment processingEnv;

    public AnnotationProcessorResourceGenerator(ProcessingEnvironment processingEnv, ServiceRegistry serviceRegistry)
    {
        super(serviceRegistry);
        this.processingEnv = processingEnv;
    }

    @Override
    protected void write(GenerableResource resource)
    {
        FileObject file;
        try
        {
            file = processingEnv.getFiler().createResource(SOURCE_OUTPUT, EMPTY, resource.getFilePath());
        }
        catch (IOException e)
        {
            throw wrapException(e, resource);
        }

        try (OutputStream out = file.openOutputStream())
        {
            out.write(resource.getContentBuilder().toString().getBytes());
            out.flush();
        }
        catch (IOException e)
        {
            throw wrapException(e, resource);
        }
    }

    private RuntimeException wrapException(Exception e, GenerableResource resource)
    {
        return new RuntimeException(String.format("Could not write generated resource '%s'", resource.getFilePath()), e);
    }

}
