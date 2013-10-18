/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.components;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO
 */
public class ReceivedHeader
{
    private String id;
    private String from;
    private String by;
    private String via;
    private String with;
    private String _for;
    private String timestamp;

    public static ReceivedHeader getInstance(String receivedHeader)
    {
        String fromPattern = "(?:from (.*?))?";
        String byPattern = "(?:by (.*?))?";
        String viaPattern = "(?:via (.*?))?";
        String withPattern = "(?:with (.*?))?";
        String idPattern = "(?:id (.*?))?";
        String forPattern = "(?:for (.*?))?";
        String timePattern = ";(.*)";

        String pattern = fromPattern + byPattern + viaPattern + withPattern + idPattern + forPattern
                         + timePattern;

        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(receivedHeader);

        ReceivedHeader result = null;
        if (matcher.find())
        {
            result = new ReceivedHeader();
            result.setFrom(matcher.group(1));
            result.setBy(matcher.group(2));
            result.setVia(matcher.group(3));
            result.setWith(matcher.group(4));
            result.setId(matcher.group(5));
            result.setFor(matcher.group(6));
            result.setTimestamp(matcher.group(7));
        }

        return result;
    }

    public String getId()
    {
        return id;
    }

    private void setId(String id)
    {
        this.id = (id != null ? id.trim() : null);
    }

    public String getFrom()
    {
        return from;
    }

    private void setFrom(String from)
    {
        this.from = (from != null ? from.trim() : null);
    }

    public String getBy()
    {
        return by;
    }

    private void setBy(String by)
    {
        this.by = (by != null ? by.trim() : null);
    }

    public String getVia()
    {
        return via;
    }

    private void setVia(String via)
    {
        this.via = (via != null ? via.trim() : null);
    }

    public String getWith()
    {
        return with;
    }

    private void setWith(String with)
    {
        this.with = (with != null ? with.trim() : null);
    }

    public String getFor()
    {
        return _for;
    }

    public void setFor(String _for)
    {
        this._for = (_for != null ? _for.trim() : null);
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    private void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp.trim();
    }

    @Override
    public String toString()
    {
        return "Received {\n  " + (getId() != null ? "id: " + getId() + "\n  " : "")
               + (getFrom() != null ? "from: " + getFrom() + "\n  " : "")
               + (getBy() != null ? "by: " + getBy() + "\n  " : "")
               + (getVia() != null ? "via: " + getVia() + "\n  " : "")
               + (getWith() != null ? "with: " + getWith() + "\n  " : "")
               + (getFor() != null ? "for: " + getFor() + "\n  " : "") + "date-time: " + getTimestamp()
               + "\n  " + "}";
    }
}
