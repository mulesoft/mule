/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api;

import java.util.List;
import java.util.Map;

/**
 * HTTP parameters map.
 *
 * Usually used to hold query parameters, form parameters or uri parameters.
 *
 * Implementations of this class are immutable.
 */
public interface HttpParameters extends Map<String, String>
{

    /**
     * Utility method to retrieve all the values for a certain parameter.
     *
     * @param key the name of the parameter.
     * @return a list with all the values for that parameter name. If there's no value then an empty collection is returned.
     */
    List<String> getAll(String key);

    /**
     * @param key parameter name
     * @return the parameter value. If there wsa a collection of values for the parameter then only the first one is returned.
     */
    @Override
    String get(Object key);

    /**
     * @return Immutable map with all the parameters which values are collections with all the values.
     */
    Map<String, ? extends List<String>> toListValuesMap();
}
