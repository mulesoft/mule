/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RequestUtils
{
    private RequestUtils()
    {
    }

    public static String executeRequestWithSocket (String requestText, int port) throws Exception
    {
        Socket socket = new Socket("localhost", port);
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.println(requestText);
        printWriter.println("");
        printWriter.flush();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String outputString;

        while((outputString = bufferedReader.readLine()) != null)
        {
            stringBuilder.append(outputString);
        }

        bufferedReader.close();
        printWriter.close();
        socket.close();

        return stringBuilder.toString();
    }

}
