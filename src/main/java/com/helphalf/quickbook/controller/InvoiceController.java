package com.helphalf.quickbook.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helphalf.quickbook.client.OAuth2PlatformClientFactory;
import com.helphalf.quickbook.qbo.ContextFactory;
import com.helphalf.quickbook.qbo.DataServiceFactory;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.Invoice;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.exception.InvalidTokenException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.helphalf.quickbook.helper.InvoiceHelper;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
public class InvoiceController {

    @Autowired
    OAuth2PlatformClientFactory factory;


    private static final Logger LOG = Logger.getLogger(QBOController.class);


    @ResponseBody
    @PostMapping(value = "/invoice/create", produces = "application/json;charset=UTF-8")
    public ResponseEntity createInvoice(@RequestBody String newInvoice) {


        Map<String, Object> newInvoiceMap = JSON.parseObject(newInvoice, Map.class);
        //项目需要用的auth
//        String realmId = (String) newInvoiceMap.get("realm_id");
//        String accessToken = (String) newInvoiceMap.get("access_token");
//        String refreshToken = (String) newInvoiceMap.get("refresh_token");


        //本地测试用auth
        String realmId = ContextFactory.companyID;
        String accessToken = ContextFactory.bearerToken;
        String refreshToken = ContextFactory.refreshToken;


        if (StringUtils.isEmpty(realmId)) {
//            return new JSONObject().put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!").toString();
            Map<String, Object> er = new HashMap<>();
            er.put("response status:", "400");
            er.put("message", "Third party quickbook error: No realm ID.  QBO calls only work if the accounting scope was passed!");
            return ResponseEntity.badRequest().body(er);
        }


        if (StringUtils.isEmpty(accessToken)) {
            Map<String, Object> er = new HashMap<>();
            er.put("response status:", "401");
            er.put("message", "Third party quickbook error: No access token.");
            return ResponseEntity.badRequest().body(er);
        }

        String failureMsg = "Failed";

        DataServiceFactory serviceClient = new DataServiceFactory();

        String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";

        try {

            // set custom config
            Config.setProperty(Config.BASE_URL_QBO, url);

            //get DataService
            DataService service = serviceClient.getDataService(realmId, accessToken);

            Invoice invoice = InvoiceHelper.createInvoiceFields(service, newInvoiceMap);
//            System.out.println("saveInvoice-----"+JSON.toJSONString(invoice));
//            Invoice savedInvoice = service.add(invoice);
//            System.out.println("saveInvoice2-----"+JSON.toJSONString(savedInvoice));
//            LOG.info("Invoice created: " + savedInvoice.getId() + " ::invoice doc num: " + savedInvoice.getDocNumber());

//            return JSON.toJSONString(invoice);

//            ObjectMapper mapper = new ObjectMapper();
//                String jsonInString = mapper.writeValueAsString(invoice);
//                Map<String,Object> map = new HashMap<String,Object>();
//                map.put("response :", invoice);

                return ResponseEntity.ok(JSON.toJSONString(invoice));

            } catch (InvalidTokenException e) {
            return ResponseEntity.badRequest().body(e);
        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
//            list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));
            return ResponseEntity.badRequest().body(list);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return (ResponseEntity) ResponseEntity.ok();
    }
//        return "";

    @ResponseBody
    @PostMapping(value = "/invoice/update", produces = "application/json;charset=UTF-8")
    public ResponseEntity updateInvoice(@RequestBody String newInvoice) {

        Map<String, Object> newInvoiceMap = JSON.parseObject(newInvoice, Map.class);
        //项目需要用的auth
//        String realmId = (String) newInvoiceMap.get("realm_id");
//        String accessToken = (String) newInvoiceMap.get("access_token");
//        String refreshToken = (String) newInvoiceMap.get("refresh_token");

        String realmId = ContextFactory.companyID;

        if (StringUtils.isEmpty(realmId)) {

            Map<String, Object> er = new HashMap<>();
            er.put("response status:", "400");
            er.put("message", "Third party quickbook error: No realm ID.  QBO calls only work if the accounting scope was passed!");

            return ResponseEntity.badRequest().body(er);
        }
        String accessToken = ContextFactory.bearerToken;
        String refreshToken = ContextFactory.refreshToken;

        String failureMsg = "Failed";

        if (StringUtils.isEmpty(accessToken)) {
            Map<String, Object> er = new HashMap<>();
            er.put("response status:", "401");
            er.put("message", "Third party quickbook error: No access token.");
            return ResponseEntity.badRequest().body(er);
        }

        DataServiceFactory serviceClient = new DataServiceFactory();

        String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";

        try {

            // set custom config
            Config.setProperty(Config.BASE_URL_QBO, url);

            //get DataService
            DataService service = serviceClient.getDataService(realmId, accessToken);

            Invoice invoice = InvoiceHelper.updateInvoice(service, newInvoiceMap);

            return ResponseEntity.ok(JSON.toJSONString(invoice));

        } catch (InvalidTokenException e) {
            return ResponseEntity.badRequest().body(e);
        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
//            list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));
            return ResponseEntity.badRequest().body(list);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return (ResponseEntity) ResponseEntity.ok();
    }



    private String processResponse(String failureMsg, QueryResult queryResult) {
        if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
            Invoice invoice =(Invoice) queryResult.getEntities().get(0);

            LOG.info("invoice -> invoice: " + invoice.getDocNumber());
            ObjectMapper mapper = new ObjectMapper();
            try {
                String jsonInString = mapper.writeValueAsString(invoice);
                return jsonInString;
            } catch (JsonProcessingException e) {
                LOG.error("Exception while getting account info ", e);
                return new JSONObject().put("response",failureMsg).toString();
            }

        }
        return failureMsg;
    }

}
