/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transformer;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

import java.util.ArrayList;
import java.util.List;

public class LifecycleTrackerTransformer extends AbstractTransformer implements Lifecycle, MuleContextAware
{

    private final List<String> tracker = new ArrayList<String>();

    public List<String> getTracker()
    {
        return tracker;
    }

    public void setProperty(final String value)
    {
        tracker.add("setProperty");
    }

    public void setMuleContext(final MuleContext context)
    {
        tracker.add("setMuleContext");
    }


    @Override
    public void initialise() throws InitialisationException
    {
        tracker.add("initialise");
    }

    public void start() throws MuleException
    {
        tracker.add("start");
    }

    public void stop() throws MuleException
    {
        tracker.add("stop");
    }

    public void dispose()
    {
        tracker.add("dispose");
    }

    @Override
    protected Object doTransform(final Object src, final String encoding)
            throws TransformerException
    {

        // dirty trick to get the transformer instance that was used for the
        // request
        return this;
    }

}

