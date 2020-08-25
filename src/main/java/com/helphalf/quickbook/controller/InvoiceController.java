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

import com.helphalf.quickbook.helper.InvoiceHelper;

import java.text.ParseException;
import java.util.List;
import java.util.Map;


@RestController
public class InvoiceController {

    @Autowired
    OAuth2PlatformClientFactory factory;


    private static final Logger LOG = Logger.getLogger(QBOController.class);


    @ResponseBody
    @PostMapping("/invoice/create")
    public String createInvoice(@RequestBody String newInvoice) {

        String realmId = ContextFactory.companyID;

        Map<String, Object> newInvoiceMap = JSON.parseObject(newInvoice, Map.class);

        if (StringUtils.isEmpty(realmId)) {
            return new JSONObject().put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!").toString();
        }
        String accessToken = ContextFactory.bearerToken;
        String refreshToken = ContextFactory.refreshToken;

        String failureMsg = "Failed";

        DataServiceFactory serviceClient = new DataServiceFactory();

        String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";

        try {

            // set custom config
            Config.setProperty(Config.BASE_URL_QBO, url);

            //get DataService
            DataService service = serviceClient.getDataService(realmId, accessToken);

            Invoice invoice = InvoiceHelper.getInvoiceFields(service,newInvoiceMap);
            System.out.println("saveInvoice-----"+JSON.toJSONString(invoice));

            Invoice savedInvoice = service.add(invoice);
            System.out.println("saveInvoice2-----"+JSON.toJSONString(savedInvoice));
            LOG.info("Invoice created: " + savedInvoice.getId() + " ::invoice doc num: " + savedInvoice.getDocNumber());

            return JSON.toJSONString(invoice);
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
                String sql = "select * from invoice";
                QueryResult queryResult = service.executeQuery(sql);
                return processResponse(failureMsg, queryResult);

            } catch (OAuthException e1) {
                LOG.error("Error while calling bearer token :: " + e.getMessage());
                return new JSONObject().put("response",failureMsg).toString();
            } catch (FMSException e1) {
                LOG.error("Error while calling account currency :: " + e.getMessage());
                return new JSONObject().put("response",failureMsg).toString();
            }

        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
            list.forEach(error -> LOG.error("Error while calling executeQuery :: " + error.getMessage()));
            return new JSONObject().put("response",failureMsg).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
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

//    private DataService getDataService(String realmId, String accessToken) throws FMSException {
//
//        //create oauth object
//        OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
//        //create context
//        Context context = new Context(oauth, ServiceType.QBO, realmId);
//
//        // create dataservice
//        return new DataService(context);
//    }

}
