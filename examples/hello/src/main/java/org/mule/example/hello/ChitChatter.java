/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
