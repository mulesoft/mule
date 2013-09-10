/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
if(!dojo._hasResource["dojox.cometd.timestamp"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.cometd.timestamp"] = true;
dojo.provide("dojox.cometd.timestamp");
dojo.require("dojox.cometd");

// A cometd extension that adds a timestamp to every message
dojox.cometd._extendOutList.push(function(msg){msg.timestamp=new Date().toUTCString();return msg});

}
