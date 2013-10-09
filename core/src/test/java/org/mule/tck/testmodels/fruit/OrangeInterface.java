/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import java.util.List;
import java.util.Map;

/**
 * TODO
 */

public interface OrangeInterface extends Fruit
{
    String getBrand();

    Integer getSegments();

    Double getRadius();

    void setBrand(String string);

    void setSegments(Integer integer);

    void setRadius(Double double1);

    List getListProperties();

    void setListProperties(List listProperties);

    Map getMapProperties();

    void setMapProperties(Map mapProperties);

    List getArrayProperties();

    void setArrayProperties(List arrayProperties);

    FruitCleaner getCleaner();

    void setCleaner(FruitCleaner cleaner);

    void wash();

    void polish();
}
