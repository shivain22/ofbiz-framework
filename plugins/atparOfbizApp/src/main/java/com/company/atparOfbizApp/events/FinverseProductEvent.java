package com.company.atparOfbizApp.events;

import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.HashMap;
import java.util.Map;

public class FinverseProductEvent {
    public static Map<String, Object> createFinverseProductEvent(DispatchContext dctx, Map<String, ?> context){
        LocalDispatcher dispatcher= dctx.getDispatcher();
        String productId =(String)context.get("productId");
        System.out.println(context);
        Map<String, Object> resultMap = new HashMap<>();

        return resultMap;
    }
}
