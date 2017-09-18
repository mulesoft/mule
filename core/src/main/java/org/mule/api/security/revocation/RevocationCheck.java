/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security.revocation;

public class RevocationCheck
{
    private StandardRevocationCheck standardRevocationCheck;
    private CustomOcspResponder customOcspResponder;
    private CrlFile crlFile;

    public StandardRevocationCheck getStandardRevocationCheck()
    {
        return standardRevocationCheck;
    }

    public void setStandardRevocationCheck(StandardRevocationCheck standardRevocationCheck)
    {
        this.standardRevocationCheck = standardRevocationCheck;
    }

    public CustomOcspResponder getCustomOcspResponder()
    {
        return customOcspResponder;
    }

    public void setCustomOcspResponder(CustomOcspResponder customOcspResponder)
    {
        this.customOcspResponder = customOcspResponder;
    }

    public CrlFile getCrlFile()
    {
        return crlFile;
    }

    public void setCrlFile(CrlFile crlFile)
    {
        this.crlFile = crlFile;
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

        RevocationCheck that = (RevocationCheck) o;

        if (standardRevocationCheck != null ? !standardRevocationCheck.equals(that.standardRevocationCheck) : that.standardRevocationCheck != null)
        {
            return false;
        }
        if (customOcspResponder != null ? !customOcspResponder.equals(that.customOcspResponder) : that.customOcspResponder != null)
        {
            return false;
        }
        return crlFile != null ? crlFile.equals(that.crlFile) : that.crlFile == null;
    }

    @Override
    public int hashCode()
    {
        int result = standardRevocationCheck != null ? standardRevocationCheck.hashCode() : 0;
        result = 31 * result + (customOcspResponder != null ? customOcspResponder.hashCode() : 0);
        result = 31 * result + (crlFile != null ? crlFile.hashCode() : 0);
        return result;
    }
}
