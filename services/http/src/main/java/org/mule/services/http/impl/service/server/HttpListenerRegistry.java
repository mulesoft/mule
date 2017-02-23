/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server;

import static org.mule.services.http.impl.service.server.grizzly.DefaultMethodRequestMatcher.getMethodsListRepresentation;
import static org.mule.services.http.impl.service.server.grizzly.HttpParser.normalizePathWithSpacesOrEncodedSpaces;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.StringUtils;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.PathAndMethodRequestMatcher;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.RequestHandlerManager;

import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpListenerRegistry implements RequestHandlerProvider {

  private static final String WILDCARD_CHARACTER = "*";
  private static final String SLASH = "/";
  private Logger logger = LoggerFactory.getLogger(getClass());

  private final ServerAddressMap<HttpServer> serverAddressToServerMap = new ServerAddressMap<>();
  private final Map<HttpServer, ServerAddressRequestHandlerRegistry> requestHandlerPerServerAddress = new HashMap<>();

  public synchronized RequestHandlerManager addRequestHandler(final HttpServer server, final RequestHandler requestHandler,
                                                              final PathAndMethodRequestMatcher requestMatcher) {
    ServerAddressRequestHandlerRegistry serverAddressRequestHandlerRegistry = this.requestHandlerPerServerAddress.get(server);
    if (serverAddressRequestHandlerRegistry == null) {
      serverAddressRequestHandlerRegistry = new ServerAddressRequestHandlerRegistry();
      requestHandlerPerServerAddress.put(server, serverAddressRequestHandlerRegistry);
      serverAddressToServerMap.put(server.getServerAddress(), server);
    }
    return serverAddressRequestHandlerRegistry.addRequestHandler(requestMatcher, requestHandler);
  }

  public synchronized void removeHandlersFor(HttpServer server) {
    requestHandlerPerServerAddress.remove(server);
    serverAddressToServerMap.remove(server.getServerAddress());
  }

  @Override
  public RequestHandler getRequestHandler(String ip, int port, final HttpRequest request) {
    if (logger.isDebugEnabled()) {
      logger.debug("Looking RequestHandler for request: " + request.getPath());
    }
    final HttpServer server = serverAddressToServerMap.get(new DefaultServerAddress(ip, port));
    if (server != null && !server.isStopping() && !server.isStopped()) {
      final ServerAddressRequestHandlerRegistry serverAddressRequestHandlerRegistry = requestHandlerPerServerAddress.get(server);
      if (serverAddressRequestHandlerRegistry != null) {
        return serverAddressRequestHandlerRegistry.findRequestHandler(request);
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("No RequestHandler found for request: " + request.getPath());
    }
    return NoListenerRequestHandler.getInstance();
  }

  public class ServerAddressRequestHandlerRegistry {

    private PathMap serverRequestHandler;
    private PathMap rootPathMap = new PathMap();
    private PathMap catchAllPathMap = new PathMap();
    private Set<String> paths = new HashSet<>();
    private LoadingCache<String, Stack<PathMap>> pathMapSearchCache =
        CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<String, Stack<PathMap>>() {

          @Override
          public Stack<PathMap> load(String path) {
            return findPossibleRequestHandlers(path);
          }
        });

    public synchronized RequestHandlerManager addRequestHandler(final PathAndMethodRequestMatcher requestMatcher,
                                                                final RequestHandler requestHandler) {
      pathMapSearchCache.invalidateAll();
      String requestMatcherPath = normalizePathWithSpacesOrEncodedSpaces(requestMatcher.getPath());
      Preconditions.checkArgument(requestMatcherPath.startsWith(SLASH) || requestMatcherPath.equals(WILDCARD_CHARACTER),
                                  "path parameter must start with /");
      validateCollision(requestMatcher);
      List<String> matcherMethods = requestMatcher.getMethodRequestMatcher().getMethods();
      paths.add(getMethodAndPath(getMethodsListRepresentation(matcherMethods), requestMatcherPath));
      PathMap currentPathMap = rootPathMap;
      final RequestHandlerMatcherPair addedRequestHandlerMatcherPair;
      final PathMap requestHandlerOwner;
      if (requestMatcherPath.equals(WILDCARD_CHARACTER)) {
        serverRequestHandler = new PathMap();
        addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
        requestHandlerOwner = serverRequestHandler;
        serverRequestHandler.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
      } else if (requestMatcherPath.equals("/*")) {
        addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
        requestHandlerOwner = catchAllPathMap;
        catchAllPathMap.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
      } else if (requestMatcherPath.equals(SLASH)) {
        addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
        requestHandlerOwner = rootPathMap;
        rootPathMap.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
      } else {
        final String[] pathParts = splitPath(requestMatcherPath);
        int insertionLevel = getPathPartsSize(requestMatcherPath);
        for (int i = 1; i < insertionLevel - 1; i++) {
          String currentPath = pathParts[i];
          PathMap pathMap = currentPathMap.getChildPathMap(currentPath, null);
          if (i != insertionLevel - 1) {
            if (pathMap == null) {
              pathMap = new PathMap();
              currentPathMap.addChildPathMap(currentPath, pathMap);
            }
          }

          currentPathMap = pathMap;
        }
        String currentPath = pathParts[insertionLevel - 1];
        PathMap pathMap = currentPathMap.getLastChildPathMap(currentPath);
        if (pathMap == null) {
          pathMap = new PathMap();
          currentPathMap.addChildPathMap(currentPath, pathMap);
        }
        if (requestMatcherPath.endsWith(WILDCARD_CHARACTER)) {
          addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
          pathMap.addWildcardRequestHandler(addedRequestHandlerMatcherPair);
          requestHandlerOwner = pathMap;
        } else {
          addedRequestHandlerMatcherPair = new RequestHandlerMatcherPair(requestMatcher, requestHandler);
          pathMap.addRequestHandlerMatcherPair(addedRequestHandlerMatcherPair);
          requestHandlerOwner = pathMap;
        }
      }
      return new DefaultRequestHandlerManager(requestHandlerOwner, addedRequestHandlerMatcherPair);
    }

    private void validateCollision(PathAndMethodRequestMatcher newListenerRequestMatcher) {
      final String newListenerRequestMatcherPath = newListenerRequestMatcher.getPath();
      final Stack<PathMap> possibleRequestHandlers = findPossibleRequestHandlersFromCache(newListenerRequestMatcherPath);
      for (PathMap possibleRequestHandler : possibleRequestHandlers) {
        final List<RequestHandlerMatcherPair> requestHandlerMatcherPairs = possibleRequestHandler.getRequestHandlerMatcherPairs();
        for (RequestHandlerMatcherPair requestHandlerMatcherPair : requestHandlerMatcherPairs) {
          final PathAndMethodRequestMatcher requestMatcher = requestHandlerMatcherPair.getRequestMatcher();
          final String possibleCollisionRequestMatcherPath = requestMatcher.getPath();
          if (isSameDepth(possibleCollisionRequestMatcherPath, newListenerRequestMatcherPath)) {
            if (newListenerRequestMatcher.getMethodRequestMatcher().intersectsWith(requestMatcher.getMethodRequestMatcher())) {
              String possibleCollisionLastPathPart = getLastPathPortion(possibleCollisionRequestMatcherPath);
              String newListenerRequestMatcherLastPathPart = getLastPathPortion(newListenerRequestMatcherPath);
              if (possibleCollisionLastPathPart.equals(newListenerRequestMatcherLastPathPart)
                  || (isCatchAllPath(possibleCollisionLastPathPart) && isCatchAllPath(newListenerRequestMatcherLastPathPart))
                  || (isCatchAllPath(possibleCollisionLastPathPart) && isUriParameter(newListenerRequestMatcherLastPathPart))
                  || (isUriParameter(possibleCollisionLastPathPart) && isCatchAllPath(newListenerRequestMatcherLastPathPart)
                      || (isUriParameter(possibleCollisionLastPathPart)
                          && isUriParameter(newListenerRequestMatcherLastPathPart)))) {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(String
                    .format("Already exists a listener matching that path and methods. Listener matching %s new listener %s",
                            requestMatcher, newListenerRequestMatcher)));
              }
            }
          }
        }
      }
    }

    public RequestHandler findRequestHandler(final HttpRequest request) {
      final String path = normalizePathWithSpacesOrEncodedSpaces(request.getPath());
      Preconditions.checkArgument(path.startsWith(SLASH), "path parameter must start with /");
      Stack<PathMap> foundPaths = findPossibleRequestHandlersFromCache(path);
      boolean methodNotAllowed = false;
      RequestHandlerMatcherPair requestHandlerMatcherPair = null;
      while (!foundPaths.empty()) {
        final PathMap pathMap = foundPaths.pop();
        List<RequestHandlerMatcherPair> requestHandlerMatcherPairs = pathMap.getRequestHandlerMatcherPairs();

        if (requestHandlerMatcherPairs == null && pathMap.getCatchAllPathMap() != null) {
          requestHandlerMatcherPairs = pathMap.getCatchAllPathMap().requestHandlerMatcherPairs;
        }
        requestHandlerMatcherPair = findRequestHandlerMatcherPair(requestHandlerMatcherPairs, request);

        if (requestHandlerMatcherPair != null) {
          break;
        }
        if (!requestHandlerMatcherPairs.isEmpty()) {
          // there were matching paths but no matching methods
          methodNotAllowed = true;
        }
      }
      if (requestHandlerMatcherPair == null) {
        if (logger.isInfoEnabled()) {
          logger.info("No listener found for request: " + getMethodAndPath(request.getMethod(), request.getPath()));
          logger.info("Available listeners are: [{}]", Joiner.on(", ").join(this.paths));
        }
        if (methodNotAllowed) {
          return NoMethodRequestHandler.getInstance();
        }
        return NoListenerRequestHandler.getInstance();
      }
      if (!requestHandlerMatcherPair.isRunning()) {
        return ServiceTemporarilyUnavailableListenerRequestHandler.getInstance();
      }
      return requestHandlerMatcherPair.getRequestHandler();
    }

    private String getMethodAndPath(String method, String path) {
      return "(" + method + ")" + path;
    }

    private Stack<PathMap> findPossibleRequestHandlersFromCache(String path) {
      return findPossibleRequestHandlers(path);
    }

    private Stack<PathMap> findPossibleRequestHandlers(String path) {
      PathMap currentPathMap = rootPathMap;
      PathMap auxPathMap = null;
      final String[] pathParts = splitPath(path);
      Stack<PathMap> foundPaths = new Stack<>();
      foundPaths.add(catchAllPathMap);
      if (path.equals(WILDCARD_CHARACTER)) {
        foundPaths.push(serverRequestHandler);
        return foundPaths;
      }
      if (path.equals(SLASH)) {
        foundPaths.push(rootPathMap);
        return foundPaths;
      }
      for (int i = 1; i < pathParts.length && currentPathMap != null; i++) {
        String currentPath = pathParts[i];
        PathMap pathMap = currentPathMap.getChildPathMap(currentPath, i < pathParts.length - 1 ? pathParts[i + 1] : null);

        if (pathMap == null) {
          addCatchAllPathMapIfNotNull(currentPathMap, foundPaths);
          pathMap = currentPathMap.getCatchAllCurrentPathMap();
        } else if (pathMap.getCatchAllPathMap() != null) {
          auxPathMap = pathMap;
        }
        if (i == pathParts.length - 1) {
          if (auxPathMap != null) {
            addCatchAllPathMapIfNotNull(auxPathMap, foundPaths);
          }
          if (pathMap != null) {
            addCatchAllPathMapIfNotNull(pathMap, foundPaths);
            foundPaths.push(pathMap);
          } else {
            addCatchAllPathMapIfNotNull(currentPathMap, foundPaths);
          }
        }
        currentPathMap = pathMap;
      }
      return foundPaths;
    }

    private void addCatchAllPathMapIfNotNull(PathMap currentPathMap, Stack<PathMap> foundPaths) {
      final PathMap catchAllPathMap = currentPathMap.getCatchAllPathMap();
      if (catchAllPathMap != null) {
        foundPaths.push(catchAllPathMap);
      }
    }

    private RequestHandlerMatcherPair findRequestHandlerMatcherPair(List<RequestHandlerMatcherPair> requestHandlerMatcherPairs,
                                                                    HttpRequest request) {
      for (RequestHandlerMatcherPair requestHandlerMatcherPair : requestHandlerMatcherPairs) {
        if (requestHandlerMatcherPair.getRequestMatcher().matches(request)) {
          return requestHandlerMatcherPair;
        }
      }
      return null;
    }
  }

  private boolean isUriParameter(String pathPart) {
    return (pathPart.startsWith("{") || pathPart.startsWith("/{")) && pathPart.endsWith("}");
  }

  private String getLastPathPortion(String possibleCollisionRequestMatcherPath) {
    final String[] parts = splitPath(possibleCollisionRequestMatcherPath);
    if (parts.length == 0) {
      return StringUtils.EMPTY;
    }
    return parts[parts.length - 1];
  }

  private boolean isSameDepth(String possibleCollisionRequestMatcherPath, String newListenerRequestMatcherPath) {
    return getPathPartsSize(possibleCollisionRequestMatcherPath) == getPathPartsSize(newListenerRequestMatcherPath);
  }

  private int getPathPartsSize(String path) {
    int pathSize = splitPath(path).length - 1;
    pathSize += (path.endsWith(SLASH) ? 1 : 0);
    return pathSize;
  }

  private String[] splitPath(String path) {
    if (path.endsWith(SLASH)) {
      // Remove the last slash
      path = path.substring(0, path.length() - 1);
    }
    return path.split(SLASH, -1);
  }

  public class PathMap {

    List<RequestHandlerMatcherPair> requestHandlerMatcherPairs = new ArrayList<>();

    private Map<String, PathMap> subPaths = new HashMap<>();
    private PathMap catchAllPathMap;
    private PathMap catchAllCurrentPathMap;

    public PathMap getCatchAllPathMap() {
      return catchAllPathMap;
    }

    public PathMap getCatchAllCurrentPathMap() {
      return catchAllCurrentPathMap;
    }

    /**
     * @param subPath the sub part of the path to find
     * @param nextSubPath the next sub part of the path. It is useful for deciding between static paths and middle-wildcard paths
     *        when they are similar.
     * @return the node with the existent mappings. null if there's no such node.
     */
    public PathMap getChildPathMap(final String subPath, String nextSubPath) {
      if (isCatchAllPath(subPath) || isUriParameter(subPath)
          || (isViablePath(subPath, nextSubPath) && !matchesNextSubPaths(subPath, nextSubPath))) {
        return getCatchAllCurrentPathMap();
      }
      PathMap pathMap = subPaths.get(subPath);
      return pathMap;
    }

    private boolean isViablePath(String subPath, String nextSubPath) {
      if (getCatchAllCurrentPathMap() != null && nextSubPath != null) {
        return getCatchAllCurrentPathMap().subPaths.containsKey(nextSubPath);
      }
      return false;
    }

    private boolean matchesNextSubPaths(String subPath, String nextSubPath) {
      if (subPaths.containsKey(subPath)) {
        PathMap nextPathMap = subPaths.get(subPath);
        if (nextPathMap.getSubPaths() != null) {
          return nextPathMap.getSubPaths().containsKey(nextSubPath);
        }
      }
      return false;
    }

    /**
     * @param subPath a sub part of the path
     * @return the node with the existent mappings. null if there's no such node.
     */
    public PathMap getLastChildPathMap(final String subPath) {
      if (isCatchAllPath(subPath) || isUriParameter(subPath)) {
        return getCatchAllCurrentPathMap();
      }
      PathMap pathMap = subPaths.get(subPath);
      return pathMap;
    }

    public void addRequestHandlerMatcherPair(final RequestHandlerMatcherPair requestHandlerMatcherPair) {
      this.requestHandlerMatcherPairs.add(requestHandlerMatcherPair);
    }

    public void addChildPathMap(final String path, final PathMap pathMap) {
      if (path.equals(WILDCARD_CHARACTER) || path.endsWith("}")) {
        catchAllCurrentPathMap = pathMap;
      } else {
        subPaths.put(path, pathMap);
      }
    }

    public Map<String, PathMap> getSubPaths() {
      return subPaths;
    }


    public List<RequestHandlerMatcherPair> getRequestHandlerMatcherPairs() {
      return requestHandlerMatcherPairs;
    }

    public void addWildcardRequestHandler(RequestHandlerMatcherPair requestHandlerMatcherPair) {
      if (this.catchAllPathMap == null) {
        this.catchAllPathMap = new PathMap();
      }
      this.catchAllPathMap.addRequestHandlerMatcherPair(requestHandlerMatcherPair);
    }

    public boolean removeRequestHandlerMatcherPair(RequestHandlerMatcherPair requestHandlerMatcherPair) {
      if (this.requestHandlerMatcherPairs.remove(requestHandlerMatcherPair)) {
        return true;
      }
      if (this.catchAllPathMap != null && this.catchAllPathMap.removeRequestHandlerMatcherPair(requestHandlerMatcherPair)) {
        return true;
      }
      if (this.catchAllCurrentPathMap != null
          && this.catchAllCurrentPathMap.removeRequestHandlerMatcherPair(requestHandlerMatcherPair)) {
        return true;
      }
      return false;
    }
  }

  private boolean isCatchAllPath(String path) {
    return WILDCARD_CHARACTER.equals(path);
  }

  public class RequestHandlerMatcherPair {

    private PathAndMethodRequestMatcher requestMatcher;
    private RequestHandler requestHandler;
    private boolean running = true;

    private RequestHandlerMatcherPair(PathAndMethodRequestMatcher requestMatcher, RequestHandler requestHandler) {
      this.requestMatcher = requestMatcher;
      this.requestHandler = requestHandler;
    }

    public PathAndMethodRequestMatcher getRequestMatcher() {
      return requestMatcher;
    }

    public RequestHandler getRequestHandler() {
      return requestHandler;
    }

    public boolean isRunning() {
      return this.running;
    }

    public void setIsRunning(Boolean running) {
      this.running = running;
    }

  }
}
