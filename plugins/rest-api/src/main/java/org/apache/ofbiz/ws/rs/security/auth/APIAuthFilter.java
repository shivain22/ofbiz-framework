/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.ws.rs.security.auth;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Priority;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.apache.ofbiz.ws.rs.common.AuthenticationScheme;
import org.apache.ofbiz.ws.rs.resources.OFBizServiceResource;
import org.apache.ofbiz.ws.rs.security.Secured;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

/**
 * Api Security
 */
@Secured
@Provider
@Priority(Priorities.AUTHORIZATION)
public class APIAuthFilter implements ContainerRequestFilter {

    private static final String MODULE = APIAuthFilter.class.getName();
    private static final String PRIVATE_TENANT_HEADER = "X-PrivateTenant";
    @Context
    private UriInfo uriInfo;

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private ServletContext servletContext;

    /**
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        String tenantId = requestContext.getHeaderString(PRIVATE_TENANT_HEADER);

        Delegator delegator = (Delegator) servletContext.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) servletContext.getAttribute("dispatcher");
        String delegatorName = "default";
        try {
            // after this line the delegator is replaced with the new per-tenant delegator
            delegator = DelegatorFactory.getDelegator(delegatorName);
            dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
        } catch (NullPointerException e) {
            Debug.logError(e, "Error getting tenant delegator", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", "Tenant [" + tenantId + "]  not found...");

        }

        if (!tenantId.isEmpty() && tenantId !=null) {
             delegatorName = getDelegatorName(tenantId, delegator, dispatcher);
            try {
                // after this line the delegator is replaced with the new per-tenant delegator
                 delegator = DelegatorFactory.getDelegator(delegatorName);
                 dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
            } catch (NullPointerException e) {
                Debug.logError(e, "Error getting tenant delegator", MODULE);
                Map<String, String> messageMap = UtilMisc.toMap("errorMessage", "Tenant [" + tenantId + "]  not found...");

            }
            httpRequest.setAttribute("dispatcher", dispatcher);
            httpRequest.setAttribute("delegator", delegator);
            servletContext.setAttribute("dispatcher", dispatcher);
            servletContext.setAttribute("delegator", delegator);
            httpRequest.setAttribute("servletContext", servletContext);
        }


        if (isServiceResource()) {
            String service = (String) RestApiUtil.extractParams(uriInfo.getPathParameters()).get("serviceName");
            if (UtilValidate.isNotEmpty(service)) {
                ModelService mdService = null;
                try {
                    mdService = WebAppUtil.getDispatcher(servletContext).getDispatchContext().getModelService(service);

                } catch (GenericServiceException e) {
                    Debug.logError(e.getMessage(), MODULE);
                }
                // Skip auth for services auth=false in service definition and if Authorization header is absent
                // Still validate the token if it is present even if service being called is auth=false
                if (mdService != null && !mdService.isAuth() && authorizationHeader == null) {
                    return;
                }
            }
        }


        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(requestContext, false, "Unauthorized: Access is denied due to invalid or absent Authorization header.");
            return;
        }

        String jwtToken = JWTManager.getHeaderAuthBearerToken(httpRequest);
        Map<String, Object> claims = JWTManager.validateToken(jwtToken, JWTManager.getJWTKey(delegator));
        if(claims.containsKey("userTenantId") &&( delegator.getDelegatorName().equals("default") || delegator.getDelegatorName().isEmpty())) {
            String userTenantId = claims.get("userTenantId").toString();
             delegatorName="default";
            if (!userTenantId.isEmpty()) {
                delegatorName = getDelegatorName(userTenantId, delegator, dispatcher);

            }
            try {
                // after this line the delegator is replaced with the new per-tenant delegator
                delegator = DelegatorFactory.getDelegator(delegatorName);
                dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
            } catch (NullPointerException e) {
                Debug.logError(e, "Error getting tenant delegator", MODULE);
                Map<String, String> messageMap = UtilMisc.toMap("errorMessage", "Tenant [" + userTenantId + "]  not found...");

            }
            httpRequest.setAttribute("dispatcher", dispatcher);
            httpRequest.setAttribute("delegator", delegator);
            servletContext.setAttribute("dispatcher", dispatcher);
            servletContext.setAttribute("delegator", delegator);
            httpRequest.setAttribute("servletContext", servletContext);

        }
        if (claims.containsKey(ModelService.ERROR_MESSAGE)) {
            abortWithUnauthorized(requestContext, true, "Unauthorized: " + (String) claims.get(ModelService.ERROR_MESSAGE));
        } else {
            GenericValue userLogin = extractUserLoginFromJwtClaim(delegator, claims);
            httpRequest.setAttribute("userLogin", userLogin);
        }
    }

    /**
     * @param authorizationHeader
     * @return
     */
    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.toLowerCase().startsWith(AuthenticationScheme.BEARER.getScheme().toLowerCase() + " ");
    }

    /**
     * @param requestContext
     */
    private void abortWithUnauthorized(ContainerRequestContext requestContext, boolean isAuthHeaderPresent, String message) {
        if (!isAuthHeaderPresent) {
            requestContext.abortWith(
                    RestApiUtil.errorBuilder(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(), message)
                    .header(HttpHeaders.WWW_AUTHENTICATE,
                    AuthenticationScheme.BEARER.getScheme() + " realm=\"" + AuthenticationScheme.REALM + "\"").build());
        } else {
            requestContext
                .abortWith(RestApiUtil.error(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(), message));
        }

    }

    private GenericValue extractUserLoginFromJwtClaim(Delegator delegator, Map<String, Object> claims) {
        String userLoginId = (String) claims.get("userLoginId");
        if (UtilValidate.isEmpty(userLoginId)) {
            Debug.logWarning("No userLoginId found in the JWT token.", MODULE);
            return null;
        }
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            if (UtilValidate.isEmpty(userLogin)) {
                Debug.logWarning("There was a problem with the JWT token. Could not find provided userLogin " + userLoginId, MODULE);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get UserLogin information from JWT Token: " + e.getMessage(), MODULE);
        }
        return userLogin;
    }

    private boolean isServiceResource() {
        return OFBizServiceResource.class.isAssignableFrom(resourceInfo.getResourceClass());
    }

    private String getDelegatorName(String tenantId,Delegator delegator,LocalDispatcher dispatcher ){

        Map<String, ?> result = null;
        String delegatorName="";
        if (UtilValidate.isNotEmpty(tenantId)) {
            // see if we need to activate a tenant delegator, only do if the current delegatorName has a hash symbol in it,
            // and if the passed in tenantId doesn't match the one in the delegatorName
            String oldDelegatorName = delegator.getDelegatorName();
            int delegatorNameHashIndex = oldDelegatorName.indexOf('#');
            String currentDelegatorTenantId = null;
            if (delegatorNameHashIndex > 0) {
                currentDelegatorTenantId = oldDelegatorName.substring(delegatorNameHashIndex + 1);
                if (currentDelegatorTenantId != null) currentDelegatorTenantId = currentDelegatorTenantId.trim();
            }

            if (delegatorNameHashIndex == -1 || (currentDelegatorTenantId != null && !tenantId.equals(currentDelegatorTenantId))) {
                // make that tenant active, setup a new delegator and a new dispatcher
                delegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;

            }
        }

        return delegatorName;
    }

}
