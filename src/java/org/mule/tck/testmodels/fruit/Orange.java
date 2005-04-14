/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.tck.testmodels.fruit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Orange implements Fruit, Callable
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(Orange.class);

    private boolean bitten = false;
    private Integer segments = new Integer(10);
    private Double radius = new Double(4.34);
    private String brand;
    

    private Map mapProperties;

    private List listProperties;

    private List arrayProperties;

    public Orange()
    {
    }

    public Orange(HashMap props) throws UMOException
    {
        setBrand((String) props.get("brand"));
        setRadius((Double) props.get("radius"));
        setSegments((Integer) props.get("segments"));
    }

    //        public Orange(String brand, Double radius, Integer segments)
    //        {
    //            this.brand = brand;
    //            this.radius = radius;
    //            this.segments = segments;
    //        }

    public void bite()
    {
        bitten = true;
    }

    public boolean isBitten()
    {
        return bitten;
    }

    public Object onCall(UMOEventContext context) throws UMOException
    {
        logger.debug("Orange received an event in UMOCallable.onEvent! Event says: " + context.getMessageAsString());
        bite();
        return null;
    }

    /**
     * @return
     */
    public String getBrand()
    {
        return brand;
    }

    /**
     * @return
     */
    public Integer getSegments()
    {
        return segments;
    }

    /**
     * @return
     */
    public Double getRadius()
    {
        return radius;
    }

    /**
     * @param string
     */
    public void setBrand(String string)
    {
        brand = string;
    }

    /**
     * @param integer
     */
    public void setSegments(Integer integer)
    {
        segments = integer;
    }

    /**
     * @param double1
     */
    public void setRadius(Double double1)
    {
        radius = double1;
    }

    /**
     * @return Returns the listProperties.
     */
    public List getListProperties()
    {
        return listProperties;
    }

    /**
     * @param listProperties The listProperties to set.
     */
    public void setListProperties(List listProperties)
    {
        this.listProperties = listProperties;
    }

    /**
     * @return Returns the mapProperties.
     */
    public Map getMapProperties()
    {
        return mapProperties;
    }

    /**
     * @param mapProperties The mapProperties to set.
     */
    public void setMapProperties(Map mapProperties)
    {
        this.mapProperties = mapProperties;
    }

    /**
     * @return Returns the arrayProperties.
     */
    public List getArrayProperties()
    {
        return arrayProperties;
    }

    /**
     * @param arrayProperties The arrayProperties to set.
     */
    public void setArrayProperties(List arrayProperties)
    {
        this.arrayProperties = arrayProperties;
    }

}