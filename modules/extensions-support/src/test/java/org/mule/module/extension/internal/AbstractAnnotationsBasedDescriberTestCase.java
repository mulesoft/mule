/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.mule.module.extension.internal.util.ExtensionsTestUtils.createManifestFileIfNecessary;
import org.mule.extension.introspection.declaration.Describer;
import org.mule.extension.introspection.declaration.fluent.Declaration;
import org.mule.extension.introspection.declaration.fluent.OperationDeclaration;
import org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.CollectionUtils;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;

public abstract class AbstractAnnotationsBasedDescriberTestCase extends AbstractMuleTestCase
{
    private Describer describer;
    private File manifest;

    @Before
    public void createResources() throws IOException
    {
        File metaInfDirectory = ExtensionsTestUtils.getMetaInfDirectory(getClass().getSuperclass());
        if (metaInfDirectory != null)
        {
            manifest = createManifestFileIfNecessary(metaInfDirectory);
        }
    }

    @After
    public void deleteResources()
    {
        if (manifest != null && manifest.exists())
        {
            FileUtils.deleteQuietly(manifest);
        }
    }

    protected Describer getDescriber()
    {
        return describer;
    }

    protected void setDescriber(Describer describer)
    {
        this.describer = describer;
    }

    protected Describer describerFor(final Class<?> type)
    {
        return new AnnotationsBasedDescriber(type);
    }

    protected OperationDeclaration getOperation(Declaration declaration, final String operationName)
    {
        return (OperationDeclaration) CollectionUtils.find(declaration.getOperations(), object -> ((OperationDeclaration) object).getName().equals(operationName));
    }
}
