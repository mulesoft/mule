/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore.web;

/**
 * Poor man's CSS stylesheet
 */
public class HtmlTemplate
{
    public static String wrapHtmlBody(String body)
    {
        String output = "";
        
        output += "<html>";
        output += "<head>";
        output += "<meta http-equiv=\"Content-Language\" content=\"en-us\"/>";
        output += "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\"/>";
        output += "<title>Bookstore Administration Console</title>";
        output += "</head>";

        output += "<body link=\"#FFFFFF\" vlink=\"#FFFFFF\" alink=\"#FFFFFF\" bgcolor=\"#990000\" text=\"#FFFFFF\">";            
        output += body;            
        output += "</body>";

        output += "<br/><a href=\"/bookstore-admin/\">Return to Home Page</a>";
        output += "</html>";
        
        return output;
    }
}


