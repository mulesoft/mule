/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.OutputHandler;
import org.mule.component.DefaultJavaComponent;
import org.mule.module.atom.server.MuleRequestContext;
import org.mule.object.SingletonObjectFactory;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;

import javax.activation.MimeType;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.FilterChain;
import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.RequestContext.Scope;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This component receives requests from Mule and passes them off to Abdera.
 */
public class AbderaServiceComponent extends DefaultJavaComponent
{
    public static final String EVENT_CONTEXT = "_muleEventContext";

    private Provider provider;

    public AbderaServiceComponent()
    {
        super();

        setObjectFactory(new SingletonObjectFactory(new AbderaCallable(this)));
    }


    public static final class AbderaCallable implements Callable
    {
        protected static transient final Log logger = LogFactory.getLog(AbderaServiceComponent.class);

        private final AbderaServiceComponent abderaServiceComponent;

        public AbderaCallable(AbderaServiceComponent abderaServiceComponent)
        {
            this.abderaServiceComponent = abderaServiceComponent;
        }

        public Object onCall(MuleEventContext event) throws MuleException
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(event.getMessageAsString());
            }

            MuleMessage msg = event.getMessage();
            IRI baseIri = initBaseUri(event.getEndpointURI());
            String contextPath = msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY, StringUtils.EMPTY);

            Provider provider = abderaServiceComponent.getProvider();
            MuleRequestContext reqcontext = new MuleRequestContext(provider,
                                                                   event,
                                                                   msg,
                                                                   contextPath,
                                                                   baseIri);
            reqcontext.setAttribute(Scope.REQUEST, EVENT_CONTEXT, event);

            FilterChain chain = new FilterChain(provider, reqcontext);

            try
            {
                return output(msg, chain.next(reqcontext), event.getMuleContext());
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        private IRI initBaseUri(URI endpointURI)
        {
            String iri = endpointURI.toString();
            if (!iri.endsWith("/"))
            {
                iri = iri + "/";
            }

            return new IRI(iri);
        }

        private MuleMessage output(final MuleMessage request,
                                   final ResponseContext context, final MuleContext muleContext) throws IOException
        {
            OutputHandler payload = new OutputHandler()
            {

                public void write(MuleEvent event, OutputStream out) throws IOException
                {
                    if (context.hasEntity())
                    {
                        context.writeTo(out);
                        out.flush();
                    }
                }

            };
            DefaultMuleMessage response = new DefaultMuleMessage(payload, muleContext);

            response.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, context.getStatus());
            long cl = context.getContentLength();
            String cc = context.getCacheControl();
            if (cl > -1)
            {
                response.setOutboundProperty("Content-Length", Long.toString(cl));
            }
            if (cc != null && cc.length() > 0)
            {
                response.setOutboundProperty("Cache-Control", cc);
            }

            MimeType ct = context.getContentType();
            if (ct != null)
            {
                response.setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, ct.toString());
            }

            String[] names = context.getHeaderNames();
            for (String name : names)
            {
                Object[] headers = context.getHeaders(name);
                for (Object value : headers)
                {
                    if (value instanceof Date)
                    {
                        throw new RuntimeException();
                    }
//                        response.setDateHeader(name, ((Date)value).getTime());
                    else
                    {
                        response.setOutboundProperty(name, value.toString());
                    }
                }
            }

            return response;
        }

    }

    public Provider getProvider()
    {
        return provider;
    }

    public void setProvider(Provider provider)
    {
        this.provider = provider;
        provider.init(Abdera.getInstance(), new HashMap<String, String>());
    }
}
