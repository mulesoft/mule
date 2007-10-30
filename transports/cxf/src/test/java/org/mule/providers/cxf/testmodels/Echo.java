
package org.mule.providers.cxf.testmodels;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService
public class Echo
{

    @WebResult(name = "text")
    @WebMethod
    public String echo(@WebParam(name = "text")
    String s)
    {
        return s;
    }
}
