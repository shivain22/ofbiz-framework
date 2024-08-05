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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.control.ContextFilter;
import org.apache.ofbiz.webapp.control.ControlServlet;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.ofbiz.ws.rs.common.AuthenticationScheme;
import org.apache.ofbiz.ws.rs.security.AuthToken;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;


@AuthToken
@Provider
public class HttpBasicAuthFilter implements ContainerRequestFilter {

    private static final String MODULE = HttpBasicAuthFilter.class.getName();

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private HttpServletResponse httpResponse;

    @Context
    private ServletContext servletContext;

    private static final String REALM = "OFBiz";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (!isBasicAuth(authorizationHeader)) {
            abortWithUnauthorized(requestContext, false, "Unauthorized: Access is denied due to invalid or absent Authorization header");
            return;
        }

        // Get request body
        String requestBody = getRequestBody(requestContext);
        JsonNode jsonNode = objectMapper.readTree(requestBody);
        String user = jsonNode.path("USERNAME").asText();
        String pass = jsonNode.path("PASSWORD").asText();
        String userTenantId = jsonNode.path("userTenantId").asText();
        LocalDispatcher dispatcher = (LocalDispatcher) servletContext.getAttribute("dispatcher");
        Delegator delegator = (Delegator) servletContext.getAttribute("delegator");

        Security security= (Security) servletContext.getAttribute("security");
        httpRequest.setAttribute("USERNAME",user);
        httpRequest.setAttribute("PASSWORD",pass);
        httpRequest.setAttribute("userTenantId",userTenantId);

        String delegatorName="";
        if(!userTenantId.isEmpty()) {
            delegatorName = getDelegatorName(userTenantId, delegator, dispatcher);
            if (!delegatorName.isEmpty()) {

                try {
                    // after this line the delegator is replaced with the new per-tenant delegator
                    delegator = DelegatorFactory.getDelegator(delegatorName);
                    dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
                } catch (NullPointerException e) {
                    Debug.logError(e, "Error getting tenant delegator", MODULE);
                    Map<String, String> messageMap = UtilMisc.toMap("errorMessage", "Tenant [" + userTenantId + "]  not found...");

                }
            }
        }
            else {
                delegatorName = "default";

                    try {
                        // after this line the delegator is replaced with the new per-tenant delegator
                        delegator = DelegatorFactory.getDelegator(delegatorName);
                        dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
                    } catch (NullPointerException e) {
                        Debug.logError(e, "Error getting tenant delegator", MODULE);
                        Map<String, String> messageMap = UtilMisc.toMap("errorMessage", "Tenant [" + userTenantId + "]  not found...");

                    }

            }
            httpRequest.setAttribute("dispatcher",dispatcher);
            httpRequest.setAttribute("delegator",delegator);
            servletContext.setAttribute("dispatcher",dispatcher);
            servletContext.setAttribute("delegator",delegator);
            httpRequest.setAttribute("servletContext",servletContext);
            httpRequest.setAttribute("security",security);


//        ControlServlet controlServlet = (ControlServlet) servletContext.getAttribute("controlServlet");
//        if(controlServlet==null){
//            controlServlet = new ControlServlet();
//            try {
//                controlServlet.doPost(httpRequest,httpResponse);
//            } catch (ServletException e) {
//                throw new RuntimeException(e);
//            }
//        }

//        AuthContextFilter contextFilter=new AuthContextFilter();
//
//        FilterChain chain = new FilterChain() {
//            @Override
//            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
//                // Chain handling logic, if needed
//            }
//        };
//        try {
//            contextFilter.doFilter(httpRequest, httpResponse, chain);
//        } catch (ServletException e) {
//            throw new RuntimeException(e);
//        }
//        String result =LoginWorker.login(httpRequest, httpResponse );

        String[] tokens = (new String(Base64.getDecoder().decode(authorizationHeader.split(" ")[1]), "UTF-8")).split(":");
        final String username = tokens[0];
        final String password = tokens[1];
        if(!user.isEmpty() && !pass.isEmpty()){


            try {
                authenticate(user, pass);
            } catch (ForbiddenException fe) {
                abortWithUnauthorized(requestContext, true, "Access Denied: " + fe.getMessage());
            }
        }
        else {

            try {
                authenticate(username, password);
            } catch (ForbiddenException fe) {
                abortWithUnauthorized(requestContext, true, "Access Denied: " + fe.getMessage());
            }
        }
    }

    private String getRequestBody(ContainerRequestContext requestContext) throws IOException {
        // Buffer the request body
        InputStream inputStream = requestContext.getEntityStream();
        String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // Reset the stream so it can be read again later
        requestContext.setEntityStream(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));
        return requestBody;
    }

    /**
     * @param authorizationHeader
     * @return
     */
    private boolean isBasicAuth(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.toLowerCase().startsWith(AuthenticationScheme.BASIC.getScheme().toLowerCase() + " ");
    }

    /**
     * @param requestContext
     */
    private void abortWithUnauthorized(ContainerRequestContext requestContext, boolean isAuthHeaderPresent, String message) {
        if (!isAuthHeaderPresent) {
            requestContext.abortWith(
                    RestApiUtil.errorBuilder(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(), message)
                            .header(HttpHeaders.WWW_AUTHENTICATE, AuthenticationScheme.BASIC.getScheme() + " realm=\"" + REALM + "\"").build());
        } else {
            requestContext
                    .abortWith(RestApiUtil.error(Response.Status.FORBIDDEN.getStatusCode(), Response.Status.FORBIDDEN.getReasonPhrase(), message));
        }
    }

    private void authenticate(String userName, String password) throws ForbiddenException {
        Map<String, Object> result = null;
        LocalDispatcher dispatcher = (LocalDispatcher) servletContext.getAttribute("dispatcher");
        try {
            result = dispatcher.runSync("userLogin",
                    UtilMisc.toMap("login.username", userName, "login.password", password, "locale", UtilHttp.getLocale(httpRequest)));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling userLogin service", MODULE);
            throw new ForbiddenException(e.getMessage());
        }
        if (!ServiceUtil.isSuccess(result)) {
            Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
            throw new ForbiddenException(ServiceUtil.getErrorMessage(result));
        }

        GenericValue userLogin = (GenericValue) result.get("userLogin");
        httpRequest.setAttribute("userLogin", userLogin);
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
