package com.helphalf.quickbook.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helphalf.quickbook.client.OAuth2PlatformClientFactory;
import com.helphalf.quickbook.qbo.ContextFactory;
import com.helphalf.quickbook.qbo.DataServiceFactory;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.SalesReceipt;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.helphalf.quickbook.helper.SalesReceiptHelper;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SalesReceiptController {

    @Autowired
    OAuth2PlatformClientFactory factory;


    private static final Logger LOG = Logger.getLogger(QBOController.class);


    @ResponseBody
    @PostMapping("/salesReceipt/create")
    public ResponseEntity createSalesReceipt(@RequestBody String newSalesReceipt) {

        Map<String, Object> newSalesReceiptMap = JSON.parseObject(newSalesReceipt, Map.class);

        //项目需要用的auth
//        String realmId = (String) newInvoiceMap.get("realm_id");
//        String accessToken = (String) newInvoiceMap.get("access_token");
//        String refreshToken = (String) newInvoiceMap.get("refresh_token");

        //本地用auth
        String realmId = ContextFactory.companyID;

        if (StringUtils.isEmpty(realmId)) {
            Map<String, Object> er = new HashMap<>();
            er.put("response status:", "400");
            er.put("message", "Third party quickbook error: No realm ID.  QBO calls only work if the accounting scope was passed!");
            return ResponseEntity.badRequest().body(er);
        }
        String accessToken = ContextFactory.bearerToken;
        String refreshToken = ContextFactory.refreshToken;


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

            SalesReceipt salesReceipt = SalesReceiptHelper.getSalesReceiptFields(service,newSalesReceiptMap);
//

            return ResponseEntity.ok(JSON.toJSONString(salesReceipt));
        }
        catch (InvalidTokenException e) {
            LOG.error("Error while calling executeQuery :: " + e.getMessage());

            //refresh tokens
            LOG.info("received 401 during account call, refreshing tokens now");
            OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();

            try {
                BearerTokenResponse bearerTokenResponse = client.refreshToken(accessToken);

                ContextFactory.bearerToken = bearerTokenResponse.getAccessToken();
                ContextFactory.refreshToken = bearerTokenResponse.getRefreshToken();

                //call company info again using new tokens
                LOG.info("calling account using new tokens");
                DataService service = serviceClient.getDataService(realmId, accessToken);

                // get all companyinfo
                String sql = "select * from SalesReceipt";
                QueryResult queryResult = service.executeQuery(sql);
//                return processResponse(failureMsg, queryResult);
                Map<String, Object> er = new HashMap<>();

                if (queryResult.getEntities().isEmpty() || queryResult.getEntities().size() == 0) {
                    er.put("response status:", "402");
                    er.put("message", "Third party quickbook error: Can not find any object.");
                }
                return ResponseEntity.badRequest().body(er);
            } catch (OAuthException e1) {
                LOG.error("Error while calling bearer token :: " + e.getMessage());
//                return new JSONObject().put("response",failureMsg).toString();
                Map<String, Object> er = new HashMap<>();
                er.put("response status:", "401");
                er.put("message", "Third party quickbook error: No access token.");
                return ResponseEntity.badRequest().body(er);

            } catch (FMSException e1) {
                LOG.error("Error while calling account currency :: " + e.getMessage());
//                return new JSONObject().put("response",failureMsg).toString();
                return ResponseEntity.badRequest().body(e1);

            }

        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
            list.forEach(error -> LOG.error("Error while calling executeQuery :: " + error.getMessage()));
//            return new JSONObject().put("response",failureMsg).toString();
            Map<String, Object> er = new HashMap<>();
            er.put("response status:", "403");
            er.put("message", "Third party quickbook error: Error while calling executeQuery! " );
            return ResponseEntity.badRequest().body(er);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return (ResponseEntity) ResponseEntity.ok();
    }

    @ResponseBody
    @PostMapping(value = "/salesReceipt/update", produces = "application/json;charset=UTF-8")
    public ResponseEntity updateSalesReceipt(@RequestBody String newSalesReceipt) {


        Map<String, Object> newSalesReceiptMap = JSON.parseObject(newSalesReceipt, Map.class);

        //项目需要用的auth
//        String realmId = (String) newInvoiceMap.get("realm_id");
//        String accessToken = (String) newInvoiceMap.get("access_token");
//        String refreshToken = (String) newInvoiceMap.get("refresh_token");

        //本地用auth
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

            SalesReceipt salesReceipt = SalesReceiptHelper.updateSalesReceipt(service, newSalesReceiptMap);

            return ResponseEntity.ok(JSON.toJSONString(salesReceipt));

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
            SalesReceipt salesReceipt =(SalesReceipt) queryResult.getEntities().get(0);

            LOG.info("SalesReceipt -> SalesReceipt: " + salesReceipt.getDocNumber());
            ObjectMapper mapper = new ObjectMapper();
            try {
                String jsonInString = mapper.writeValueAsString(salesReceipt);
                return jsonInString;
            } catch (JsonProcessingException e) {
                LOG.error("Exception while getting account info ", e);
                return new JSONObject().put("response",failureMsg).toString();
            }
        }
        return failureMsg;
    }

}
