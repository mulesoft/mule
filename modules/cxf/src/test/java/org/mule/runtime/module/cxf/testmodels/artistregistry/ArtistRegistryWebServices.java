/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.testmodels.artistregistry;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(targetNamespace = "http://example.cxf.module.mule.org/", name="ArtistRegistryWebServices")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface ArtistRegistryWebServices
{
    @WebMethod
    public void addArtist(@WebParam(partName = "arg0", name="arg0") Artist arg0);

    @WebResult(name = "return", targetNamespace = "http://example.cxf.module.mule.org/", partName = "return")
    @WebMethod
    public ArtistArray getAll(@WebParam(partName = "pageSize", name="pageSize") int pageSize, @WebParam(partName = "pageNumber", name="pageNumber") int pageNumber);
}
