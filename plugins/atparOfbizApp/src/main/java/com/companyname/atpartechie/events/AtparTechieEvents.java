package com.companyname.atpartechie.events;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

public class AtparTechieEvents {

    public static final String module = AtparTechieEvents.class.getName();

    public static String createAtparTechieEvent(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        String atparTechiesUploadId = request.getParameter("atparTechiesUploadId");
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String role = request.getParameter("role");

        if (UtilValidate.isEmpty(firstName) || UtilValidate.isEmpty(lastName) || UtilValidate.isEmpty(role) ) {
            String errMsg = "First Name , Last Name and Role are required fields on the form and can't be empty.";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String comments = request.getParameter("comments");

        request.setAttribute("comments", comments);
        request.setAttribute("atparTechiesUploadId", atparTechiesUploadId);
        request.setAttribute("firstName", firstName);
        request.setAttribute("lastName", firstName + lastName);


        try {
            Debug.logInfo("=======Creating atparTechie record in event using service createAtparTechieByGroovyService=========", module);
            dispatcher.runSync("createAtparTechieByGroovyService", UtilMisc.toMap("atparTechiesUploadId", atparTechiesUploadId,
                    "firstName", firstName, "lastName", firstName + lastName, "role" , role , "comments", comments, "userLogin", userLogin));
        } catch (GenericServiceException e) {
            String errMsg = "Unable to create new records in atparTechie entity: " + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        request.setAttribute("_EVENT_MESSAGE_", "Atpar Techie created succesfully.");
        return "success";
    }
}
