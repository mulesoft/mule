dojo.require("dojox.cometd");
dojo.require("dojox.cometd.timestamp");
dojo.require("dojox.uuid.generateRandomUuid");


var mule = {

    _replyToChannels : new Array(),

    uri: '/cometd/cometd',

    /* set this to 0.0.0.0 for all local adapters or 127.0.0.1 for loopback */
    localAdapter: 'localhost',

    _init: function()
    {
        var loc = new String(document.location);
        loc = loc.replace("localhost", mule.localAdapter);
        loc = loc.substring(0, loc.indexOf("/", 8)) + mule.uri;
        console.debug("initing now: " + loc);
        dojox.cometd.init(loc);

        // handle cometd failures
        if (mule._meta)
        {
            dojo.unsubscribe(mule._meta, null, null);
        }
    },


    _dispose: function()
    {
        console.debug("disposing now");

        for (c in mule._replyToChannels)
        {
            mule.unsubscribe(c, null);
        }

        if (mule._meta)
        {
            dojo.unsubscribe(mule._meta);
        }
        mule._meta = null;
        //dojox.cometd.unsubscribe("/notification", mule);
        dojox.cometd.disconnect();
    },

    subscribe: function(channel, callback)
    {
        console.debug("subscribe:" + channel + ", " + callback);
        dojox.cometd.subscribe(channel, mule, callback);
    },

    unsubscribe: function(channel, callback)
    {
        console.debug("unsubscribe:" + channel + ", " + callback);
        dojox.cometd.unsubscribe(channel, mule, callback);
    },

    publish: function(channel, data)
    {
        console.debug("publish:" + channel + ", " + data);
        dojox.cometd.publish(channel, data);
    },

    rpc: function(channel, data, callback)
    {
        var replyTo = channel + "#" + dojox.cometd.clientId;
        console.debug("RPC:" + channel + ", " + data);
        console.debug("RPC: setting replyTo: " + replyTo);

        var message = new Object();
        message.payload = data;
        message.replyTo = replyTo;

        if(mule._replyToChannels.indexOf(replyTo) == -1)
        {
            console.debug("Mule RPC: creating subscription for client: " + replyTo);
            dojox.cometd.subscribe(replyTo, mule, callback);
            mule._replyToChannels[mule._replyToChannels.length] = replyTo;
            console.debug("Mule RPC: subscriptions are: " + mule._replyToChannels.toString());
        }
        var messageJson = dojo.toJson(message);
        console.debug("message is: " + messageJson);
        dojox.cometd.publish(channel, message);

    }

    //TODO we could add remote dispatcher support pretty easy here. Not today though :)
    //    remoteDispatcher: {
    //        send: function(endpoint, data) {
    //
    //        }
    //    }
};

dojo.addOnLoad(mule, "_init");
dojo.addOnUnload(mule, "_dispose");


