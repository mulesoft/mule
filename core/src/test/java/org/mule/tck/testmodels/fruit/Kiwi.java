/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.api.MuleEventContext;

/**
 * A test object not implementing Callable, but having a matching method accepting
 * MuleEventContext.
 */
public class Kiwi implements Fruit
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1468423665948468954L;

    private boolean bitten;

    public void handle(MuleEventContext eventContext) throws Exception
    {
        final Object payload = eventContext.getMessage().getPayload();
        if (payload instanceof FruitLover)
        {
            this.bite();
        }
    }

    public void bite()
    {
        this.bitten = true;
    }

    public boolean isBitten()
    {
        return this.bitten;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Kiwi))
        {
            return false;
        }

        Kiwi kiwi = (Kiwi) o;

        if (bitten != kiwi.bitten)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return (bitten ? 1 : 0);
    }
}
