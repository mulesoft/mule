/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.hello;

/**
 * <code>ChitChatter</code> TODO (document class)
 */
public class ChitChatter
{
    private String chitchat = "";

    public ChitChatter()
    {
        chitchat = LocaleMessage.getGreetingPart2();
    }

    public void chat(ChatString string)
    {
        string.append(chitchat);
    }

}
