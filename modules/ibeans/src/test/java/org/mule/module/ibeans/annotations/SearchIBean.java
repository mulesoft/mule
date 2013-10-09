/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
