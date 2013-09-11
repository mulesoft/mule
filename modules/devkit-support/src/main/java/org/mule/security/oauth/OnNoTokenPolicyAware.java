/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

public interface OnNoTokenPolicyAware
{

    /**
     * @return a non-null instance of {@link org.mule.security.oauth.OnNoTokenPolicy}
     *         that specifies the behavior to take when token is not set
     */
    public OnNoTokenPolicy getOnNoTokenPolicy();

    public void setOnNoTokenPolicy(OnNoTokenPolicy policy);
}
