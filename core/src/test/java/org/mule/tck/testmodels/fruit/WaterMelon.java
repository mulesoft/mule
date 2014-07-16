/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WaterMelon implements Fruit, Startable, Stoppable, Disposable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8860598811203869100L;

    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(WaterMelon.class);

    private boolean bitten = false;
    private Integer seeds = new Integer(100);
    private Double radius = new Double(4.34);
    private String brand;
    private String state = "void";

    public WaterMelon()
    {
        super();
    }

    public WaterMelon(HashMap props) throws MuleException
    {
        logger.info("Initialisaing Water melon with hashmap constructor");
        setBrand((String) props.get("namespace.brand"));
        setRadius((Double) props.get("another.namespace.radius"));
        setSeeds((Integer) props.get("seeds"));
        state = "initialised";
    }

    public void bite()
    {
        bitten = true;
    }

    public boolean isBitten()
    {
        return bitten;
    }

    public void myEventHandler(MuleEvent event) throws MuleException
    {
        logger.debug("Water Melon received an event in MyEventHandler! MuleEvent says: "
                     + event.getMessageAsString());
        bite();
    }

    public String getBrand()
    {
        return brand;
    }

    public Integer getSeeds()
    {
        return seeds;
    }

    public Double getRadius()
    {
        return radius;
    }

    public void setBrand(String string)
    {
        brand = string;
    }

    public void setSeeds(Integer integer)
    {
        seeds = integer;
    }

    public void setRadius(Double double1)
    {
        radius = double1;
    }

    public String getState()
    {
        return state;
    }

    public void start()
    {
        state = "started";
    }

    public void stop()
    {
        state = "stopped";
    }

    public void dispose()
    {
        state = "disposed";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof WaterMelon)
        {
            WaterMelon melon = (WaterMelon) obj;
            return (getBrand().equals(melon.getBrand()) && getRadius().equals(melon.getRadius())
                    && getSeeds().equals(melon.getSeeds()) && getState().equals(getState()));
        }

        return super.equals(obj);
    }

    @Override
    public int hashCode()
    {
        int result;
        result = (bitten ? 1 : 0);
        result = 31 * result + seeds.hashCode();
        result = 31 * result + radius.hashCode();
        result = 31 * result + (brand != null ? brand.hashCode() : 0);
        result = 31 * result + state.hashCode();
        return result;
    }
}
