/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.api.MuleContext;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.module.launcher.application.ArtifactMuleContextDelegate;

public abstract class ArtifactMuleContextBuilder extends DefaultMuleContextBuilder
{

    private final ArtifactMuleContextDelegate muleContextDelegate = new ArtifactMuleContextDelegate();
    private MuleContext muleContext;

    @Override
    public MuleContext buildMuleContext()
    {
        muleContext = super.buildMuleContext();
        configureClassLoaderMuleContext(muleContext);
        return muleContextDelegate;
    }

    @Override
    protected MuleContext createMuleContextToInject(MuleContext muleContext)
    {
        return muleContextDelegate;
    }

    /**
     * Allows to create a relation between the artifact and the real MuleContext
     *
     * @param muleContext
     */
    protected abstract void configureClassLoaderMuleContext(MuleContext muleContext);

    public MuleContext getMuleRealContext()
    {
        return muleContext;
    }
}