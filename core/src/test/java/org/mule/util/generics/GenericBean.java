/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.generics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 */
public class GenericBean<T>
{

    enum CustomEnum
    {

        VALUE_1, VALUE_2;

        public String toString()
        {
            return "CustomEnum: " + name();
        }
    }

    private Set<Integer> integerSet;

    private List<String> resourceList;

    private List<List<Integer>> listOfLists;

    private ArrayList<String[]> listOfArrays;

    private List<Map<Integer, Long>> listOfMaps;

    private Map plainMap;

    private Map<Short, Integer> shortMap;

    private HashMap<Long, ?> longMap;

    private Map<Number, Collection<? extends Object>> collectionMap;

    private Map<String, Map<Integer, Long>> mapOfMaps;

    private Map<Integer, List<Integer>> mapOfLists;

    private CustomEnum customEnum;

    private T genericProperty;

    private List<T> genericListProperty;


    public GenericBean()
    {
    }

    public GenericBean(Set<Integer> integerSet)
    {
        this.integerSet = integerSet;
    }

    public GenericBean(Set<Integer> integerSet, List<String> resourceList)
    {
        this.integerSet = integerSet;
        this.resourceList = resourceList;
    }

    public GenericBean(HashSet<Integer> integerSet, Map<Short, Integer> shortMap)
    {
        this.integerSet = integerSet;
        this.shortMap = shortMap;
    }

    public GenericBean(Map<Short, Integer> shortMap, String resource)
    {
        this.shortMap = shortMap;
        this.resourceList = Collections.singletonList(resource);
    }

    public GenericBean(Map plainMap, Map<Short, Integer> shortMap)
    {
        this.plainMap = plainMap;
        this.shortMap = shortMap;
    }

    public GenericBean(HashMap<Long, ?> longMap)
    {
        this.longMap = longMap;
    }

    public GenericBean(boolean someFlag, Map<Number, Collection<? extends Object>> collectionMap)
    {
        this.collectionMap = collectionMap;
    }


    public Set<Integer> getIntegerSet()
    {
        return integerSet;
    }

    public void setIntegerSet(Set<Integer> integerSet)
    {
        this.integerSet = integerSet;
    }

    public List<String> getResourceList()
    {
        return resourceList;
    }

    public void setResourceList(List<String> resourceList)
    {
        this.resourceList = resourceList;
    }

    public List<List<Integer>> getListOfLists()
    {
        return listOfLists;
    }

    public ArrayList<String[]> getListOfArrays()
    {
        return listOfArrays;
    }

    public void setListOfArrays(ArrayList<String[]> listOfArrays)
    {
        this.listOfArrays = listOfArrays;
    }

    public void setListOfLists(List<List<Integer>> listOfLists)
    {
        this.listOfLists = listOfLists;
    }

    public List<Map<Integer, Long>> getListOfMaps()
    {
        return listOfMaps;
    }

    public void setListOfMaps(List<Map<Integer, Long>> listOfMaps)
    {
        this.listOfMaps = listOfMaps;
    }

    public Map getPlainMap()
    {
        return plainMap;
    }

    public Map<Short, Integer> getShortMap()
    {
        return shortMap;
    }

    public void setShortMap(Map<Short, Integer> shortMap)
    {
        this.shortMap = shortMap;
    }

    public HashMap<Long, ?> getLongMap()
    {
        return longMap;
    }

    public void setLongMap(HashMap<Long, ?> longMap)
    {
        this.longMap = longMap;
    }

    public Map<Number, Collection<? extends Object>> getCollectionMap()
    {
        return collectionMap;
    }

    public void setCollectionMap(Map<Number, Collection<? extends Object>> collectionMap)
    {
        this.collectionMap = collectionMap;
    }

    public Map<String, Map<Integer, Long>> getMapOfMaps()
    {
        return mapOfMaps;
    }

    public void setMapOfMaps(Map<String, Map<Integer, Long>> mapOfMaps)
    {
        this.mapOfMaps = mapOfMaps;
    }

    public Map<Integer, List<Integer>> getMapOfLists()
    {
        return mapOfLists;
    }

    public void setMapOfLists(Map<Integer, List<Integer>> mapOfLists)
    {
        this.mapOfLists = mapOfLists;
    }

    public T getGenericProperty()
    {
        return genericProperty;
    }

    public void setGenericProperty(T genericProperty)
    {
        this.genericProperty = genericProperty;
    }

    public List<T> getGenericListProperty()
    {
        return genericListProperty;
    }

    public void setGenericListProperty(List<T> genericListProperty)
    {
        this.genericListProperty = genericListProperty;
    }

    public CustomEnum getCustomEnum()
    {
        return customEnum;
    }

    public void setCustomEnum(CustomEnum customEnum)
    {
        this.customEnum = customEnum;
    }


    public static GenericBean createInstance(Set<Integer> integerSet)
    {
        return new GenericBean(integerSet);
    }

    public static GenericBean createInstance(Set<Integer> integerSet, List<String> resourceList)
    {
        return new GenericBean(integerSet, resourceList);
    }

    public static GenericBean createInstance(HashSet<Integer> integerSet, Map<Short, Integer> shortMap)
    {
        return new GenericBean(integerSet, shortMap);
    }

    public static GenericBean createInstance(Map<Short, Integer> shortMap, String resource)
    {
        return new GenericBean(shortMap, resource);
    }

    public static GenericBean createInstance(Map map, Map<Short, Integer> shortMap)
    {
        return new GenericBean(map, shortMap);
    }

    public static GenericBean createInstance(HashMap<Long, ?> longMap)
    {
        return new GenericBean(longMap);
    }

    public static GenericBean createInstance(boolean someFlag, Map<Number, Collection<? extends Object>> collectionMap)
    {
        return new GenericBean(someFlag, collectionMap);
    }

}
