/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.util.Preconditions;
import org.mule.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpListenerRegistry implements RequestHandlerProvider
{

    private static final String WILDCARD_CHARACTER = "*";
    private static final String SLASH = "/";
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final ServerAddressMap<Server> serverAddressToServerMap = new ServerAddressMap<>();
    private final Map<Server, ServerAddressRequestHandlerRegistry> requestHandlerPerServerAddress = new HashMap<>();

    public synchronized RequestHandlerManager addRequestHandler(final Server server, final RequestHandler requestHandler, final ListenerRequestMatcher requestMatcher)
    {
        ServerAddressRequestHandlerRegistry serverAddressRequestHandlerRegistry = this.requestHandlerPerServerAddress.get(server);
        if (serverAddressRequestHandlerRegistry == null)
        {
            serverAddressRequestHandlerRegistry = new ServerAddressRequestHandlerRegistry();
            requestHandlerPerServerAddress.put(server, serverAddressRequestHandlerRegistry);
            serverAddressToServerMap.put(server.getServerAddress(), server);
        }
        return serverAddressRequestHandlerRegistry.addRequestHandler(requestMatcher, requestHandler);
    }

    @Override
    public RequestHandler getRequestHandler(String host, int port, final HttpRequest request)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Looking RequestHandler for request: " + request.getPath());
        }
        final Server server = serverAddressToServerMap.get(new ServerAddress(host, port));
        if ((!server.isStopping() && !server.isStopped()))
        {
            final ServerAddressRequestHandlerRegistry serverAddressRequestHandlerRegistry = requestHandlerPerServerAddress.get( server);
            if (serverAddressRequestHandlerRegistry != null)
            {
                return serverAddressRequestHandlerRegistry.findRequestHandler(request);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("No RequestHandler found for request: " + request.getPath());
            }
        }
        return NoListenerRequestHandler.getInstance();
    }

    public class ServerAddressRequestHandlerRegistry
    {

        private PathMap serverRequestHandler;
        private PathMap rootPathMap = new PathMap();
        private PathMap catchAllPathMap = new PathMap();
        private LoadingCache<String, Stack<PathMap>> pathMapSearchCache = CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<String, Stack<PathMap>>() {
            public Stack<PathMap> load(String path) {
                return findPossibleRequestHandlers(path);
            }
        });

        public synchronized RequestHandlerManager addRequestHandler(final ListenerRequestMatcher requestMatcher, final RequestHandler requestHandler)
        {
            pathMapSearchCache.invalidateAll();
            String requestMatcherPath = requestMatcher.getPath();
            Preconditions.checkArgument(requestMatcherPath.startsWith(SLASH) || requestMatcherPath.equals(WILDCARD_CHARACTER), "path parameter must start with /");
            validateCollision(requestMatcher);
            PathMap currentPathMap = rootPathMap;
            final RequestHandlerMatcherPair addedRequestHandlerMatcherPair;
            final PathMap requestHandlerOwner;
            if (requestMatcherPath.equals(WILDCARD_CHARACTER))
            {
                serverRequestHandler = new PathMap();
                addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
                requestHandlerOwner = serverRequestHandler;
                serverRequestHandler.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
            }
            else if (requestMatcherPath.equals("/*"))
            {
                addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
                requestHandlerOwner = catchAllPathMap;
                catchAllPathMap.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
            }
            else if (requestMatcherPath.equals(SLASH))
            {
                addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
                requestHandlerOwner = rootPathMap;
                rootPathMap.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
            }
            else
            {
                final String[] pathParts = requestMatcherPath.split(SLASH);
                int insertionLevel = getPathPartsSize(requestMatcherPath);
                for (int i = 1; i < insertionLevel - 1; i++)
                {
                    String currentPath = pathParts[i];
                    PathMap pathMap = currentPathMap.getChildPathMap(currentPath);
                    if (i != insertionLevel - 1)
                    {
                        if (pathMap == null)
                        {
                            pathMap = new PathMap();
                            currentPathMap.addChildPathMap(currentPath, pathMap);
                        }
                    }

                    currentPathMap = pathMap;
                }
                String currentPath = pathParts[insertionLevel - 1];
                PathMap pathMap = currentPathMap.getLastChildPathMap(currentPath);
                if (pathMap == null)
                {
                    pathMap = new PathMap();
                    currentPathMap.addChildPathMap(currentPath, pathMap);
                }
                if (requestMatcherPath.endsWith(WILDCARD_CHARACTER))
                {
                    addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
                    pathMap.addWildcardRequestHandler(addedRequestHandlerMatcherPair);
                    requestHandlerOwner = pathMap;
                }
                else
                {
                    addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
                    pathMap.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
                    requestHandlerOwner = pathMap;
                }
            }
            return new DefaultRequestHandlerManager(requestHandlerOwner, addedRequestHandlerMatcherPair);
        }

        private void validateCollision(ListenerRequestMatcher newListenerRequestMatcher)
        {
            final String newListenerRequestMatcherPath = newListenerRequestMatcher.getPath();
            final Stack<PathMap> possibleRequestHandlers = findPossibleRequestHandlersFromCache(newListenerRequestMatcherPath);
            for (PathMap possibleRequestHandler : possibleRequestHandlers)
            {
                final List<RequestHandlerMatcherPair> requestHandlerMatcherPairs = possibleRequestHandler.getRequestHandlerMatcherPairs();
                for (RequestHandlerMatcherPair requestHandlerMatcherPair : requestHandlerMatcherPairs)
                {
                    final ListenerRequestMatcher requestMatcher = requestHandlerMatcherPair.getRequestMatcher();
                    final String possibleCollisionRequestMatcherPath = requestMatcher.getPath();
                    if (isSameDepth(possibleCollisionRequestMatcherPath, newListenerRequestMatcherPath))
                    {
                        if (newListenerRequestMatcher.getMethodRequestMatcher().intersectsWith(requestMatcher.getMethodRequestMatcher()))
                        {
                            String possibleCollisionLastPathPart = getLastPathPortion(possibleCollisionRequestMatcherPath);
                            String newListenerRequestMatcherLastPathPart  = getLastPathPortion(newListenerRequestMatcherPath);
                            if (possibleCollisionLastPathPart.equals(newListenerRequestMatcherLastPathPart) ||
                                (isCatchAllPath(possibleCollisionLastPathPart) && isCatchAllPath(newListenerRequestMatcherLastPathPart)) ||
                                (isCatchAllPath(possibleCollisionLastPathPart) && isUriParameter(newListenerRequestMatcherLastPathPart)) ||
                                (isUriParameter(possibleCollisionLastPathPart) && isCatchAllPath(newListenerRequestMatcherLastPathPart) ||
                                 (isUriParameter(possibleCollisionLastPathPart) && isUriParameter(newListenerRequestMatcherLastPathPart))))
                            {
                                throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("Already exists a listener matching that path and methods. Listener matching %s new listener %s", requestMatcher, newListenerRequestMatcher)));
                            }
                        }
                    }
                }
            }
        }

        public RequestHandler findRequestHandler(final HttpRequest request)
        {
            final String path = request.getPath();
            Preconditions.checkArgument(path.startsWith(SLASH), "path parameter must start with /");
            Stack<PathMap> foundPaths = findPossibleRequestHandlersFromCache(path);

            RequestHandlerMatcherPair requestHandlerMatcherPair = null;
            while (!foundPaths.empty())
            {
                final PathMap pathMap = foundPaths.pop();
                requestHandlerMatcherPair = findRequestHandlerMatcherPair(pathMap.getRequestHandlerMatcherPairs(), request);
                if (requestHandlerMatcherPair != null)
                {
                    break;
                }
            }
            if (requestHandlerMatcherPair == null)
            {
                return NoListenerRequestHandler.getInstance();
            }
            if (!requestHandlerMatcherPair.isRunning())
            {
                return ServiceTemporarilyUnavailableListenerRequestHandler.getInstance();
            }
            return requestHandlerMatcherPair.getRequestHandler();
        }

        private Stack<PathMap> findPossibleRequestHandlersFromCache(String path)
        {
            return findPossibleRequestHandlers(path);
        }

        private Stack<PathMap> findPossibleRequestHandlers(String path)
        {
            PathMap currentPathMap = rootPathMap;
            final String[] pathParts = path.split(SLASH);
            Stack<PathMap> foundPaths = new Stack<>();
            foundPaths.add(catchAllPathMap);
            if (path.equals(WILDCARD_CHARACTER))
            {
                foundPaths.push(serverRequestHandler);
                return foundPaths;
            }
            if (path.equals(SLASH))
            {
                foundPaths.push(rootPathMap);
                return foundPaths;
            }
            for (int i = 1; i < pathParts.length && currentPathMap != null; i++)
            {
                String currentPath = pathParts[i];
                PathMap pathMap = currentPathMap.getChildPathMap(currentPath);
                if (pathMap == null)
                {
                    addCatchAllPathMapIfNotNull(currentPathMap, foundPaths);
                    pathMap = currentPathMap.getCatchAllCurrentPathMap();
                }
                if (i == pathParts.length - 1)
                {
                    if (pathMap != null)
                    {
                        foundPaths.push(pathMap);
                        addCatchAllPathMapIfNotNull(pathMap, foundPaths);
                    }
                    else
                    {
                        addCatchAllPathMapIfNotNull(currentPathMap, foundPaths);
                    }
                }
                currentPathMap = pathMap;
            }
            return foundPaths;
        }

        private void addCatchAllPathMapIfNotNull(PathMap currentPathMap, Stack<PathMap> foundPaths)
        {
            final PathMap catchAllPathMap = currentPathMap.getCatchAllPathMap();
            if (catchAllPathMap != null)
            {
                foundPaths.push(catchAllPathMap);
            }
        }

        private RequestHandlerMatcherPair findRequestHandlerMatcherPair(List<RequestHandlerMatcherPair> requestHandlerMatcherPairs, HttpRequest request)
        {
            for (RequestHandlerMatcherPair requestHandlerMatcherPair : requestHandlerMatcherPairs)
            {
                if (requestHandlerMatcherPair.getRequestMatcher().matches(request))
                {
                    return requestHandlerMatcherPair;
                }
            }
            return null;
        }
    }

    private boolean isUriParameter(String pathPart)
    {
        return (pathPart.startsWith("{") || pathPart.startsWith("/{")) && pathPart.endsWith("}");
    }

    private String getLastPathPortion(String possibleCollisionRequestMatcherPath)
    {
        final String[] parts = possibleCollisionRequestMatcherPath.split(SLASH);
        if (parts.length == 0)
        {
            return StringUtils.EMPTY;
        }
        return parts[parts.length - 1];
    }

    private boolean isSameDepth(String possibleCollisionRequestMatcherPath, String newListenerRequestMatcherPath)
    {
        return getPathPartsSize(possibleCollisionRequestMatcherPath) == getPathPartsSize(newListenerRequestMatcherPath);
    }

    private int getPathPartsSize(String path)
    {
        int pathSize = path.split(SLASH).length - 1;
        pathSize += (path.endsWith(SLASH) ? 1 : 0);
        return pathSize;
    }

    public class PathMap
    {

        List<RequestHandlerMatcherPair> requestHandlerMatcherPairs = new ArrayList<>();

        private Map<String, PathMap> subPaths = new HashMap<>();
        private PathMap catchAllPathMap;
        private PathMap catchAllCurrentPathMap;

        public PathMap getCatchAllPathMap()
        {
            return catchAllPathMap;
        }

        public PathMap getCatchAllCurrentPathMap()
        {
            return catchAllCurrentPathMap;
        }

        /**
         * @param subPath a sub part of the path
         * @return the node with the existent mappings. null if there's no such node.
         */
        public PathMap getChildPathMap(final String subPath)
        {
            if (isCatchAllPath(subPath))
            {
                return getCatchAllPathMap();
            }
            if (isUriParameter(subPath))
            {
                return getCatchAllCurrentPathMap();
            }
            PathMap pathMap = subPaths.get(subPath);
            return pathMap;
        }

        /**
         * @param subPath a sub part of the path
         * @return the node with the existent mappings. null if there's no such node.
         */
        public PathMap getLastChildPathMap(final String subPath)
        {
            if (isCatchAllPath(subPath) || isUriParameter(subPath))
            {
                return getCatchAllCurrentPathMap();
            }
            PathMap pathMap = subPaths.get(subPath);
            return pathMap;
        }

        public void addRequestHandlerMatcherPair(final RequestHandlerMatcherPair requestHandlerMatcherPair)
        {
            this.requestHandlerMatcherPairs.add(requestHandlerMatcherPair);
        }

        public void addChildPathMap(final String path, final PathMap pathMap)
        {
            if (path.equals(WILDCARD_CHARACTER) || path.endsWith("}"))
            {
                catchAllCurrentPathMap = pathMap;
            }
            else
            {
                subPaths.put(path, pathMap);
            }
        }

        public List<RequestHandlerMatcherPair> getRequestHandlerMatcherPairs()
        {
            return requestHandlerMatcherPairs;
        }

        public void addWildcardRequestHandler(RequestHandlerMatcherPair requestHandlerMatcherPair)
        {
            if (this.catchAllPathMap == null)
            {
                this.catchAllPathMap = new PathMap();
            }
            this.catchAllPathMap.addRequestHandlerMatcherPair(requestHandlerMatcherPair);
        }

        public boolean removeRequestHandlerMatcherPair(RequestHandlerMatcherPair requestHandlerMatcherPair)
        {
            if (this.requestHandlerMatcherPairs.remove(requestHandlerMatcherPair))
            {
                return true;
            }
            if (this.catchAllPathMap != null && this.catchAllPathMap.removeRequestHandlerMatcherPair(requestHandlerMatcherPair))
            {
                return true;
            }
            if (this.catchAllCurrentPathMap != null && this.catchAllCurrentPathMap.removeRequestHandlerMatcherPair(requestHandlerMatcherPair))
            {
                return true;
            }
            return false;
        }
    }

    private boolean isCatchAllPath(String path)
    {
        return WILDCARD_CHARACTER.equals(path);
    }

    public class RequestHandlerMatcherPair
    {

        private ListenerRequestMatcher requestMatcher;
        private RequestHandler requestHandler;
        private boolean running = true;

        private RequestHandlerMatcherPair(ListenerRequestMatcher requestMatcher, RequestHandler requestHandler)
        {
            this.requestMatcher = requestMatcher;
            this.requestHandler = requestHandler;
        }

        public ListenerRequestMatcher getRequestMatcher()
        {
            return requestMatcher;
        }

        public RequestHandler getRequestHandler()
        {
            return requestHandler;
        }

        public boolean isRunning()
        {
            return this.running;
        }

        public void setIsRunning(Boolean running)
        {
            this.running = running;
        }

    }
}
