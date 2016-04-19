/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

public abstract class AbstractAnnotationsBasedDescriberTestCase extends AbstractMuleTestCase
{

    private Describer describer;

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
        return new AnnotationsBasedDescriber(type, new StaticVersionResolver(getProductVersion()));
    }

    protected ExtensionDeclarer describeExtension()
    {
        return getDescriber().describe(new DefaultDescribingContext());
    }

    protected OperationDeclaration getOperation(ExtensionDeclaration extensionDeclaration, final String operationName)
    {
        return (OperationDeclaration) CollectionUtils.find(extensionDeclaration.getOperations(), object -> ((OperationDeclaration) object).getName().equals(operationName));
    }
}
