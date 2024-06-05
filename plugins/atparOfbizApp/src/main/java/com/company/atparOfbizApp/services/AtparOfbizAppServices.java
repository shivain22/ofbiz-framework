package com.companyname.atparOfbizApp.services;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.LocalDispatcher;


public class AtparOfbizAppServices {

    public static final String module = AtparOfbizAppServices.class.getName();

 //   public static Map<String, Object> createAtparTechie(DispatchContext dctx, Map<String, ? extends Object> context) {
//        Map<String, Object> result = ServiceUtil.returnSuccess();
//        Delegator delegator = dctx.getDelegator();
//        try {
//            GenericValue atparTechie = delegator.makeValue("atparTechie");
//            // Auto generating next sequence of ofbizDemoId primary key
//            atparTechie.setNextSeqId();
//            // Setting up all non primary key field values from context map
//            atparTechie.setNonPKFields(context);
//            // Creating record in database for OfbizDemo entity for prepared value
//            atparTechie = delegator.create(atparTechie);
//            result.put("atparTechieId", atparTechie.getString("atparTechieId"));
//            Debug.log("==========This is my first Java Service implementation in Apache OFBiz. AtparTechie record created successfully with atparTechieId: "+atparTechie.getString("atparTechieId"));
//
//
//            try {
//                Debug.logInfo("=======Creating OfbizDemo record in event using service createOfbizDemoByGroovyService=========", module);
//                dispatcher.runSync("createAtparTechie", UtilMisc.toMap("atparTechieUploadId", atparTechieUploadId,"firstName", firstName1, "lastName", lastName , "role" , role, "comments", comments, "userLogin", userLogin));
//            } catch (GenericServiceException e) {
//                String errMsg = "Unable to create new records in OfbizDemo entity: " + e.toString();
//                request.setAttribute("_ERROR_MESSAGE_", errMsg);
//                return ServiceUtil.returnError(errMsg);
//            }
//        } catch (GenericEntityException e) {
//            Debug.logError(e, module);
//            return ServiceUtil.returnError("Error in creating record in OfbizDemo entity ........" +module);
//        }
//        return result;
 //   }
}
