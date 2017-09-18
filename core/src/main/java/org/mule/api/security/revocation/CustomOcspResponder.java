/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security.revocation;

public class CustomOcspResponder
{
    private String url;
    private String certAlias;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getCertAlias()
    {
        return certAlias;
    }

    public void setCertAlias(String certAlias)
    {
        this.certAlias = certAlias;
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

        CustomOcspResponder that = (CustomOcspResponder) o;

        if (url != null ? !url.equals(that.url) : that.url != null)
        {
            return false;
        }
        return certAlias != null ? certAlias.equals(that.certAlias) : that.certAlias == null;
    }

    @Override
    public int hashCode()
    {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (certAlias != null ? certAlias.hashCode() : 0);
        return result;
    }
}
