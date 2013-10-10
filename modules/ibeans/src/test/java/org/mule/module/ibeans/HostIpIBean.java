/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans;

import org.mule.transformer.types.MimeTypes;

import org.ibeans.annotation.Call;
import org.ibeans.annotation.Namespace;
import org.ibeans.annotation.Return;
import org.ibeans.annotation.State;
import org.ibeans.annotation.Template;
import org.ibeans.annotation.Usage;
import org.ibeans.annotation.filter.ExpressionErrorFilter;
import org.ibeans.annotation.param.ReturnType;
import org.ibeans.annotation.param.UriParam;
import org.ibeans.api.CallException;
import org.ibeans.api.channel.HTTP;

@Usage("Simply pass in the ip address that you want to resolve and an XML document " +
        "is returned with the geo locations. The format can be found here: " +
        "http://api.hostip.info/?ip=12.215.42.19")
//using regex error filter because the core cannot depend on the XML module
@ExpressionErrorFilter(eval = "regex", expr = "Co-ordinates are unavailable", mimeType = MimeTypes.XML)
public interface HostIpIBean
{
    @Namespace("gml")
    public static final String GML_NS = "http://www.opengis.net/gml";

//    @Call(uri = "http://api.hostip.info?ip={ip}", properties = HTTP.GET)
//    public String getHostInfo(@UriParam("ip") String ip) throws CallException;

    @Call(uri = "http://api.hostip.info?ip={ip}", properties = HTTP.GET)
    @Return("#[xpath2://gml:coordinates]")
    public String getHostInfoName(@UriParam("ip") String ip) throws CallException;

    @Call(uri = "http://api.hostip.info?ip={ip}", properties = HTTP.GET)
    @Return("#[xpath2:[boolean]count(//ip) = 1]")
    public Boolean hasIp(@UriParam("ip") String ip) throws CallException;


    @ReturnType
    public static final Class DEFAULT_RETURN_TYPE = String.class;

    @State
    void init(@ReturnType Class returnType);

    @Call(uri = "http://api.hostip.info?ip={ip}")
    public <T> T getHostInfo(@UriParam("ip") String ip) throws CallException;

    @Template("one two {number}")
    public String dummyTemplateMethod(@UriParam("number") String number);
}
