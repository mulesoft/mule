/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import org.mule.extension.introspection.declaration.Describer;
import org.mule.extension.introspection.declaration.fluent.Declaration;
import org.mule.extension.introspection.declaration.fluent.OperationDeclaration;
import org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.CollectionUtils;

import org.apache.commons.collections.Predicate;

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
        return new AnnotationsBasedDescriber(type);
    }

    protected OperationDeclaration getOperation(Declaration declaration, final String operationName)
    {
        return (OperationDeclaration) CollectionUtils.find(declaration.getOperations(), new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return ((OperationDeclaration) object).getName().equals(operationName);
            }
        });
    }
}
