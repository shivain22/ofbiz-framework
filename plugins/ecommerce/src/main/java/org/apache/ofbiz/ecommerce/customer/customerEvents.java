package org.apache.ofbiz.ecommerce.customer;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.ws.rs.security.auth.HttpBasicAuthFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ForbiddenException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class customerEvents {
    private static final String MODULE = customerEvents.class.getName();

    public static Map<String, String> loginCustomer(DispatchContext ctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");

        request.setAttribute("dispatcher",dispatcher);
        request.setAttribute("delegator",delegator);
        request.setAttribute("locale",locale);

        if(request.getAttribute("USERNAME")==null){
            request.setAttribute("USERNAME",context.get("USERNAME"));
        }
        if(request.getAttribute("PASSWORD")==null){
            request.setAttribute("PASSWORD",context.get("PASSWORD"));
        }
        Map<String, String> outputMap = new HashMap<>();
        String result = JWTManager.getAuthenticationJwtToken(request,response );
        if(!result.equals("error")) {
            outputMap.put("message", "success");
            outputMap.put("token", result);
            return outputMap;
        }
        outputMap.put("message", "error");
        outputMap.put("token", null);
        return outputMap;
    }

    public static Map<String, String> registerCustomer(DispatchContext ctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");

        request.setAttribute("dispatcher", dispatcher);
        request.setAttribute("locale", locale);
        Map<String, Object> personUserLoginContext = new HashMap<>();

        // Extracting fields as represented in the XML
        String username = (String) context.get("USERNAME");
        String password = (String) context.get("PASSWORD");
        personUserLoginContext.put("userLoginId", username); // Corresponds to userLoginContext.userLoginId
        personUserLoginContext.put("currentPassword", password); // Corresponds to userLoginContext.currentPassword
        personUserLoginContext.put("currentPasswordVerify", context.get("CONFIRM_PASSWORD")); // Corresponds to newUserLogin.currentPassword (and assumes it matches currentPassword)
        personUserLoginContext.put("passwordHint", context.get("PASSWORD_HINT")); // Corresponds to userLoginContext.passwordHint

        // Create a map for personal details to be used in createPersonAndUserLogin service
        Map<String, Object> personContext = new HashMap<>();
        personContext.put("firstName", context.get("USER_FIRST_NAME")); // USER_FIRST_NAME
        personContext.put("middleName", context.get("USER_MIDDLE_NAME")); // USER_MIDDLE_NAME
        personContext.put("lastName", context.get("USER_LAST_NAME")); // USER_LAST_NAME
        personContext.put("personalTitle", context.get("USER_TITLE")); // USER_TITLE
        personContext.put("suffix", context.get("USER_SUFFIX")); // USER_SUFFIX
        personContext.put("birthDate", context.get("USER_BIRTHDATE")); // USER_BIRTHDATE
        personContext.put("gender", context.get("USER_GENDER")); // USER_GENDER

        // Add personal details to the user login context
        personUserLoginContext.putAll(personContext);

        Map<String, String> outputMap = new HashMap<>();

        try {
            // Call createPersonAndUserLogin service
            Map<String, Object> personResult = dispatcher.runSync("createPersonAndUserLogin", personUserLoginContext);
            if (ServiceUtil.isError(personResult)) {
                outputMap.put("registrationStatus", "Error");
                outputMap.put("customerId", "");
                return outputMap; // Return error status
            }
            String partyId = (String) personResult.get("partyId");
            Map<String, Object> createdUserLogin = (Map<String, Object>) personResult.get("newUserLogin");

            // Handle login and shopping cart assignment

            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();

//                LoginWorker.doBasicLogin((GenericValue) createdUserLogin, request, response);
//                LoginWorker.autoLoginSet(request, response);
                ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");

                if (cart != null) {
                    cart.setOrderPartyId(partyId);
                    request.getSession().setAttribute("shoppingCart", cart);
                }


            // Address handling
            if ("true".equals(context.get("USE_ADDRESS"))) {
                Map<String, Object> addressContext = new HashMap<>();
                addressContext.put("address1", context.get("address1"));
                addressContext.put("postalCode", context.get("postalCode"));
                addressContext.put("city", context.get("city"));
                addressContext.put("userLogin",userLogin);
                Map<String, Object> addressResult = dispatcher.runSync("createPartyPostalAddress", addressContext);
                if (ServiceUtil.isError(addressResult)) {
                    outputMap.put("registrationStatus", "Error");
                    outputMap.put("customerId", "");
                    return outputMap; // Return error status
                }
                String contactMechId = (String) addressResult.get("contactMechId");
                setContactMechPurpose(dispatcher, partyId, contactMechId, "SHIPPING_LOCATION",userLogin);
                setContactMechPurpose(dispatcher, partyId, contactMechId, "GENERAL_LOCATION",userLogin);
            }

            // Create telecom numbers
            createPartyTelecom(dispatcher, partyId, "CUSTOMER_HOME_CONTACT", (String) context.get("CUSTOMER_HOME_CONTACT"), "PHONE_HOME",userLogin);
            createPartyTelecom(dispatcher, partyId, "CUSTOMER_WORK_CONTACT", (String) context.get("CUSTOMER_WORK_CONTACT"), "PHONE_WORK",userLogin);
            createPartyTelecom(dispatcher, partyId, "CUSTOMER_FAX_CONTACT", (String) context.get("CUSTOMER_FAX_CONTACT"), "FAX_NUMBER",userLogin);
            createPartyTelecom(dispatcher, partyId, "CUSTOMER_MOBILE_CONTACT", (String) context.get("CUSTOMER_MOBILE_CONTACT"), "PHONE_MOBILE",userLogin);

            // Creating email address
            String email = (String) context.get("CUSTOMER_EMAIL");
            if (email != null && !email.isEmpty()) {
                Map<String, Object> emailContext = new HashMap<>();
                emailContext.put("emailAddress", email);
                emailContext.put("userLogin", userLogin);
                Map<String, Object> emailResult = dispatcher.runSync("createPartyEmailAddress", emailContext);
                if (ServiceUtil.isError(emailResult)) {
                    outputMap.put("registrationStatus", "Error");
                    outputMap.put("customerId", "");
                    return outputMap; // Return error status
                }
                String emailContactMechId = (String) emailResult.get("contactMechId");
                setContactMechPurpose(dispatcher, partyId, emailContactMechId, "PRIMARY_EMAIL",userLogin);
            }

            // Adding user to security group
            Map<String, Object> securityParams = new HashMap<>();
            securityParams.put("userLoginId", createdUserLogin.get("userLoginId"));
            securityParams.put("groupId", "ECOMMERCE_CUSTOMER");
            securityParams.put("userLogin",userLogin);
            dispatcher.runSync("addUserLoginToSecurityGroup", securityParams);

            // Assigning Product Store Role
            Map<String, Object> createProductStoreRoleMap = new HashMap<>();
            createProductStoreRoleMap.put("userLogin", userLogin);
            createProductStoreRoleMap.put("partyId", partyId);
            createProductStoreRoleMap.put("roleTypeId", "CUSTOMER");
            createProductStoreRoleMap.put("productStoreId", context.get("emailProductStoreId"));

            Map<String, Object> storeRoleResult = dispatcher.runSync("createProductStoreRole", createProductStoreRoleMap);
            if (ServiceUtil.isError(storeRoleResult)) {
                outputMap.put("registrationStatus", "Error");
                outputMap.put("customerId", "");
                return outputMap; // Return error status
            }

            // Sending registration email asynchronously if needed
            if ("true".equals(context.get("sendEmail"))) {
                Map<String, Object> emailParams = new HashMap<>();
                emailParams.put("sendTo", email);
                emailParams.put("subject", "Welcome to our store!");
                emailParams.put("userLogin",userLogin);
                dispatcher.runAsync("sendMailFromScreen", emailParams, true);
            }

            // Creating and assigning club number if REQUIRE_CLUB is true
            if ("true".equals(context.get("REQUIRE_CLUB"))) {
                String clubNumber = (String) context.get("CLUB_NUMBER");
                if (clubNumber == null) {
                    clubNumber = PartyWorker.createClubId(delegator, "999", 13);
                }
                Map<String, Object> personVo = new HashMap<>();
                personVo.put("partyId", partyId);
                personVo.put("memberId", clubNumber);
                personVo.put("userLogin",userLogin);
                Map<String, Object> clubResult = dispatcher.runSync("storeValue", personVo);
                if (ServiceUtil.isError(clubResult)) {
                    outputMap.put("registrationStatus", "Error");
                    outputMap.put("customerId", "");
                    return outputMap; // Return error status
                }
            }

            // Successful registration
            outputMap.put("registrationStatus", "Success");
            outputMap.put("customerId", partyId);
        } catch (Exception e) {
            e.printStackTrace();
            outputMap.put("registrationStatus", "Error");
            outputMap.put("customerId", "");
        }

        return outputMap; // Return the final output map
    }

    private static void createPartyTelecom(LocalDispatcher dispatcher, String partyId, String phoneParam, String phoneNumber, String purposeTypeId,GenericValue userLogin) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            try {
                Map<String, Object> phoneContext = new HashMap<>();
                phoneContext.put("contactNumber", phoneNumber);
                phoneContext.put("partyId", partyId);
                phoneContext.put("userLogin",userLogin);
                Map<String, Object> phoneResult = dispatcher.runSync("createPartyTelecomNumber", phoneContext);
                String contactMechId = (String) phoneResult.get("contactMechId");
                setContactMechPurpose(dispatcher, partyId, contactMechId, purposeTypeId,userLogin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void setContactMechPurpose(LocalDispatcher dispatcher, String partyId, String contactMechId, String purposeTypeId,GenericValue userLogin) {
        try {
            Map<String, Object> purposeContext = new HashMap<>();
            purposeContext.put("partyId", partyId);
            purposeContext.put("contactMechId", contactMechId);
            purposeContext.put("contactMechPurposeTypeId", purposeTypeId);
            purposeContext.put("userLogin",userLogin);
            dispatcher.runSync("createPartyContactMechPurpose", purposeContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> addToCart(DispatchContext ctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");

        request.setAttribute("dispatcher",dispatcher);
        request.setAttribute("delegator",delegator);
        request.setAttribute("locale",locale);

        if(request.getAttribute("add_product_id")==null){
            request.setAttribute("add_product_id",context.get("add_product_id"));
        }
        if(request.getAttribute("clearSearch")==null){
            request.setAttribute("clearSearch",context.get("clearSearch"));
        }
        if(request.getAttribute("mainSubmitted")==null){
            request.setAttribute("mainSubmitted",context.get("mainSubmitted"));
        }
        if(request.getAttribute("quantity")==null){
            request.setAttribute("quantity",context.get("quantity"));
        }
        if(request.getAttribute("productStoreId")==null){
            request.setAttribute("productStoreId",context.get("productStoreId"));
        }
        Map<String, String> result=  new HashMap<>();

        result.put("message",ShoppingCartEvents.addToCart(request,response));
        return result;
    }

}
