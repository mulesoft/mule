/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.param;

import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.runtime.core.api.MuleEvent;

import java.util.List;

/**
 * Resolves the values of a template query for a given event
 */
public interface ParamValueResolver {

  /**
   * Resolves query parameters
   *
   * @param muleEvent      event used to evaluate any parameter value expression
   * @param templateParams parameters defined in the query template
   * @return the list of resolved parameters to use. Non null
   */
  List<QueryParamValue> resolveParams(MuleEvent muleEvent, List<QueryParamValue> templateParams);

}
