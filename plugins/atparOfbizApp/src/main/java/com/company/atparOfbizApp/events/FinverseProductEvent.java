package com.company.atparOfbizApp.events;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.security.auth.HttpBasicAuthFilter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Context;
import java.util.HashMap;
import java.util.Map;

public class FinverseProductEvent {

    @Context
    private static HttpServletRequest httpRequest;

    private static final String MODULE = FinverseProductEvent.class.getName();

    public static Map<String, Object> createFinverseProductEvent(DispatchContext dctx, Map<String, ?> context) throws GenericServiceException {
        LocalDispatcher dispatcher= dctx.getDispatcher();
        Map<String,?> request1 = (Map<String, ?>)context.get("requestMap");
        Map<String,?> requestMap = (Map<String,?>)request1.get("request");
        String productName= (String)requestMap.get("name");
        String productIdName= (String)requestMap.get("shortName");
        String productId=productIdName+productName;
        String longDescription= productName;
        String productTypeId= "GOOD";
        String primaryProductCategoryId= "BEST-SELL-1";
        String introductionDate = String.valueOf(UtilDateTime.nowTimestamp());

        //get admin userLogin
        Map<String, Object> result = new HashMap<>();
        try {
            result = dispatcher.runSync("userLogin",
                    UtilMisc.toMap("login.username", "admin", "login.password", "ofbiz", "locale", UtilHttp.getLocale(httpRequest)));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling userLogin service", MODULE);
            throw new ForbiddenException(e.getMessage());
        }
        if (!ServiceUtil.isSuccess(result)) {
            Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
            throw new ForbiddenException(ServiceUtil.getErrorMessage(result));
        }

        GenericValue userLogin = (GenericValue) result.get("userLogin");

        Map<String, Object> inMapProduct = new HashMap<>();
        inMapProduct.put("productId",productId);
        inMapProduct.put("longDescription",longDescription);
        inMapProduct.put("internalName",productName);
        inMapProduct.put("productTypeId",productTypeId);
        inMapProduct.put("primaryProductCategoryId",primaryProductCategoryId);
        inMapProduct.put("introductionDate",introductionDate);
        inMapProduct.put("userLogin",userLogin);


        Map<String, Object> inMapCategory = new HashMap<>();
        inMapCategory.put("productId",productId);
        inMapCategory.put("productCategoryId",primaryProductCategoryId);
        inMapCategory.put("userLogin",userLogin);
        Map<String, Object> resultMap=null;
        try {
            resultMap = dispatcher.runSync("createProduct", inMapProduct);


            dispatcher.runSync("addProductToCategory", inMapCategory);

            return resultMap;
        }catch (Exception e){
            e.printStackTrace();
        }

        return resultMap;
    }
}
