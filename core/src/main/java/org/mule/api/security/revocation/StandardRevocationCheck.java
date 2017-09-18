/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security.revocation;

public class StandardRevocationCheck
{
    private Boolean onlyEndEntities = false;
    private Boolean preferCrls = false;
    private Boolean noFallback = false;
    private Boolean softFail = false;

    public Boolean getOnlyEndEntities()
    {
        return onlyEndEntities;
    }

    public void setOnlyEndEntities(Boolean onlyEndEntities)
    {
        this.onlyEndEntities = onlyEndEntities;
    }

    public Boolean getPreferCrls()
    {
        return preferCrls;
    }

    public void setPreferCrls(Boolean preferCrls)
    {
        this.preferCrls = preferCrls;
    }

    public Boolean getNoFallback()
    {
        return noFallback;
    }

    public void setNoFallback(Boolean noFallback)
    {
        this.noFallback = noFallback;
    }

    public Boolean getSoftFail()
    {
        return softFail;
    }

    public void setSoftFail(Boolean softFail)
    {
        this.softFail = softFail;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        StandardRevocationCheck that = (StandardRevocationCheck) o;

        if (onlyEndEntities != null ? !onlyEndEntities.equals(that.onlyEndEntities) : that.onlyEndEntities != null)
        {
            return false;
        }
        if (preferCrls != null ? !preferCrls.equals(that.preferCrls) : that.preferCrls != null)
        {
            return false;
        }
        if (noFallback != null ? !noFallback.equals(that.noFallback) : that.noFallback != null)
        {
            return false;
        }
        return softFail != null ? softFail.equals(that.softFail) : that.softFail == null;
    }

    @Override
    public int hashCode()
    {
        int result = onlyEndEntities != null ? onlyEndEntities.hashCode() : 0;
        result = 31 * result + (preferCrls != null ? preferCrls.hashCode() : 0);
        result = 31 * result + (noFallback != null ? noFallback.hashCode() : 0);
        result = 31 * result + (softFail != null ? softFail.hashCode() : 0);
        return result;
    }
}
