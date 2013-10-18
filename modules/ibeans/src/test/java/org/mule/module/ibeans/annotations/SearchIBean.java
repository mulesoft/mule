/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;


import java.net.URL;

import org.ibeans.annotation.Call;
import org.ibeans.annotation.Return;
import org.ibeans.annotation.param.UriParam;
import org.ibeans.api.CallException;

public interface SearchIBean
{
    //deliberate 404 error
    @Call(uri = "http://www.google.com/searchX?q={term}")
    public String searchGoogle(@UriParam("term") String searchTerm) throws CallException;

    @Call(uri = "http://www.google.com/search?q={term}")
    @Return("#[header:ibeans.call.uri]")
    public String searchGoogleAndReturnURLString(@UriParam("term") String searchTerm) throws CallException;    
    
    @Call(uri = "http://www.google.com/search?q={term}")
    @Return("#[header:ibeans.call.uri]")
    public URL searchGoogleAndReturnURL(@UriParam("term") String searchTerm) throws CallException;    
    
    @Call(uri = "http://www.google.com/search?q={term}")
    public void searchGoogleAndReturnVoid(@UriParam("term") String searchTerm) throws CallException;    
    
    @Call(uri = "http://search.yahoo.com/search?p={term}")
    public String searchYahoo(@UriParam("term") String searchTerm) throws Exception;

    @Call(uri = "http://www.ask.com/web?q={term}&search=search")
    public String searchAsk(@UriParam("term") String searchTerm) throws CallException;

    @Call(uri = "http://www.ask.com/web?q={term}&search=search")
    @Return("#[header:ibeans.call.uri]")
    public String searchAskAndReturnURLString(@UriParam("term") String searchTerm) throws CallException;

    @Call(uri = "http://www.ask.com/web?q={term}&search=search")
    @Return("#[header:ibeans.call.uri]")
    public URL searchAskAndReturnURL(@UriParam("term") String searchTerm) throws CallException;

    //IBEANS-184 : make sure we can handle void methods
    @Call(uri = "http://www.ask.com/web?q={term}&search=search")
    public void searchAskAndReturnVoid(@UriParam("term") String searchTerm) throws CallException;
}
