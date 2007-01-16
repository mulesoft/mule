/*
 * $Id:
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.handlers;

import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.Vector;

import javax.security.auth.callback.CallbackHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.util.WSSecurityUtil;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.exchange.AbstractMessage;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.handler.Handler;
import org.codehaus.xfire.handler.Phase;
import org.codehaus.xfire.security.wss4j.AbstractWSS4JHandler;
import org.codehaus.xfire.soap.handler.ReadHeadersHandler;
import org.codehaus.xfire.util.dom.DOMInHandler;
import org.w3c.dom.Document;
import org.mule.umo.security.SecurityException;

public class MuleWSSInHandler extends AbstractWSS4JHandler implements Handler
{
    /**
     * logger used by this class
     */
    protected static final Log log = LogFactory.getLog(MuleWSSInHandler.class);

    public MuleWSSInHandler()
    {
        super();
        setPhase(Phase.PARSE);
        getBefore().add(ReadHeadersHandler.class.getName());
        getAfter().add(DOMInHandler.class.getName());
    }

    public MuleWSSInHandler(Properties properties)
    {
        this();
        setProperties(properties);
    }

    /**
     * The method invoke performs the security checks on the soap headers for the
     * incoming message.
     */
    public void invoke(MessageContext msgContext)
        throws SecurityException, XFireFault, WSSecurityException
    {
        boolean doDebug = log.isDebugEnabled();

        if (doDebug)
        {
            log.debug("MuleWSSInSecurityHandler: enter invoke()");
        }

        RequestData reqData = new RequestData();

        try
        {
            reqData.setMsgContext(msgContext);

            Vector actions = new Vector();
            String action = null;

            // the action property in the security header is necessary to know which
            // type of security measure to adopt. It cannot be null.
            if ((action = (String)getOption(WSHandlerConstants.ACTION)) == null)
            {
                action = getString(WSHandlerConstants.ACTION, msgContext);
            }
            if (action == null)
            {
                throw new XFireRuntimeException("MuleWSSInHandler: No action defined");
            }

            int doAction = WSSecurityUtil.decodeAction(action, actions);

            String actor = (String)getOption(WSHandlerConstants.ACTOR);

            AbstractMessage sm = msgContext.getCurrentMessage();
            Document doc = (Document)sm.getProperty(DOMInHandler.DOM_MESSAGE);

            if (doc == null)
                throw new XFireRuntimeException("DOMInHandler must be enabled for WS-Security!");

            // Check if it's a response and if its a fault it doesn't continue.
            if (sm.getBody() instanceof XFireFault) return;

            // Get the password using a callback handler
            CallbackHandler cbHandler = null;
            if ((doAction & (WSConstants.ENCR | WSConstants.UT)) != 0)
            {
                cbHandler = getPasswordCB(reqData);
            }

            // Get and check the parameters pertaining to the signature and
            // encryption actions. Doesn't get SAML properties, though
            doReceiverAction(doAction, reqData);

            // If we're using signed SAML, we need to get the signature file in order
            // to decrypt the SAML token
            if (action.equals(WSHandlerConstants.SAML_TOKEN_SIGNED))
            {
                reqData.setSigCrypto(loadSignatureCrypto(reqData));
            }

            Vector wsResult = null;

            // process the security header
            try
            {
                wsResult = secEngine.processSecurityHeader(doc, actor, cbHandler, reqData
                    .getSigCrypto(), reqData.getDecCrypto());
            }
            catch (WSSecurityException ex)
            {
                throw new XFireFault("MuleWSSInHandler: security processing failed", ex,
                    XFireFault.SENDER);
            }

            // no security header found we check whether the action was set to
            // "no_security" or else we throw an exception
            if (wsResult == null)
            {
                if (doAction == WSConstants.NO_SECURITY)
                {
                    return;
                }
                else
                {
                    throw new XFireFault(
                        "MuleWSSInHandler: Request does not contain required Security header",
                        XFireFault.SENDER);
                }
            }

            // confim that the signature is valid
            if (reqData.getWssConfig().isEnableSignatureConfirmation())
            {
                checkSignatureConfirmation(reqData, wsResult);
            }

            // Extract the signature action result from the action vector
            WSSecurityEngineResult actionResult = WSSecurityUtil.fetchActionResult(wsResult,
                WSConstants.SIGN);

            if (actionResult != null)
            {
                X509Certificate returnCert = actionResult.getCertificate();

                if (returnCert != null)
                {
                    if (!verifyTrust(returnCert, reqData))
                    {
                        throw new XFireFault(
                            "MuleWSSInHandler: The certificate used for the signature is not trusted",
                            XFireFault.SENDER);
                    }
                }
            }

            if (actions.elementAt(0).equals(new Integer(16)))
            {
                actions.clear();
                actions.add(new Integer(2));
                actions.add(new Integer(8));
            }

            // now check the security actions: do they match, in right order?
            if (!checkReceiverResults(wsResult, actions))
            {
                throw new XFireFault(
                    "MuleWSSInHandler: security processing failed (actions mismatch)",
                    XFireFault.SENDER);

            }
            /*
             * Construct and setup the security result structure. The service may
             * fetch this and check it.
             */
            Vector results = null;
            if ((results = (Vector)msgContext.getProperty(WSHandlerConstants.RECV_RESULTS)) == null)
            {
                results = new Vector();
                msgContext.setProperty(WSHandlerConstants.RECV_RESULTS, results);
            }
            WSHandlerResult rResult = new WSHandlerResult(actor, wsResult);
            results.add(0, rResult);

            if (doDebug)
            {
                log.debug("MuleWSSInHandler: exit invoke()");
            }
        }
        catch (WSSecurityException e)
        {
            throw new WSSecurityException(e.getErrorCode());
        }
        finally
        {
            reqData.clear();
            reqData = null;
        }
    }
}
