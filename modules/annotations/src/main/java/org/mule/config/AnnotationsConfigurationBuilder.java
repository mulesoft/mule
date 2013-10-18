/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.MuleContext;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.endpoint.RegistryBackedAnnotationsParserFactory;

/**
 * Enables Mule annotation processing so that annotated objects registered with the
 * Mule registry will automatically be configured. This helper also enables JSR-330
 * injection annotations <code>javax.inject.Inject</code> and
 * <code>javax.inject.Named</code>.
 * <p/>
 * Internal Implementation note: We could have used a 'registry-bootstrap.properties'
 * file to load the objects necessary to enable annotations however, that method
 * would not allow the annotation processors to be easily overridden when using other
 * platforms such as Google App Engine.
 *
 * @since 3.0
 */
public class AnnotationsConfigurationBuilder extends AbstractConfigurationBuilder
{
    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        // Make the annotation parsers available
        AnnotationsParserFactory factory = createAnnotationsParserFactory();
        muleContext.getRegistry().registerObject("_" + factory.getClass().getSimpleName(), factory);
    }

    protected AnnotationsParserFactory createAnnotationsParserFactory()
    {
        return new RegistryBackedAnnotationsParserFactory();
    }
}
