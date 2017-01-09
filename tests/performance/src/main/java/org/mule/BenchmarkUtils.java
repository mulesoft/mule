package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextFactory;
import org.mule.context.DefaultMuleContextFactory;

/**
 * Static util methods for use in benchmark setup/teardown.  Benchmark methods themselves should ideally be self-contained for
 * clarity.
 */
public class BenchmarkUtils
{

    public static MuleContext createMuleContext() throws MuleException
    {
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        return muleContextFactory.createMuleContext();
    }

}
