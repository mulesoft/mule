/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.http.internal;

import static java.util.stream.Collectors.toCollection;

import org.mule.service.http.api.domain.ParameterMap;
import org.mule.runtime.core.util.CaseInsensitiveMapWrapper;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * {@link ParameterMap} where the key's case is not taken into account when looking for it, adding or aggregating it.
 *
 * @since 4.0
 */
public class CaseInsensitiveParameterMap extends ParameterMap {

  public CaseInsensitiveParameterMap(ParameterMap paramsMap) {
    this.paramsMap = new CaseInsensitiveMapWrapper<>(LinkedHashMap.class);
    for (String key : paramsMap.keySet()) {
      this.paramsMap.put(key, paramsMap.getAll(key).stream().collect(toCollection(LinkedList::new)));
    }
  }

}
