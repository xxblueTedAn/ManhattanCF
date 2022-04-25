package com.xxblue.mcf.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xxblue.mcf.model.CFImageResult;
import com.xxblue.mcf.model.CFVideoResult;
import com.xxblue.mcf.utils.DateUtils;
import com.xxblue.mcf.utils.ExchangeRestTemplate;
import com.xxblue.mcf.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.xxblue.mcf.utils.Utils.toJson;


@Slf4j
public class CloudFlareService {

    private RestTemplate restTemplate;

    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ssZ";

    private int connectionTimeout = 5000;  // default: 10 sec.

    private int readTimeout = 10000;        // default: 20 sec.

    private String X_AUTH_KEY = "";
    private String X_AUTH_EMAIL = "";
    private String ACCOUNT_UID = "";
    private String API_TOKEN = "";


    private HttpComponentsClientHttpRequestFactory getHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectTimeout(connectionTimeout);
        httpRequestFactory.setReadTimeout(readTimeout);
        httpRequestFactory.setHttpClient(createHttpClientAcceptsUntrustedCerts());
        return httpRequestFactory;
    }

    private CloseableHttpClient createHttpClientAcceptsUntrustedCerts() {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        return httpClient;
    }

    /**
     * 메시지 컨버터 설정
     * @return
     */
    private List<HttpMessageConverter<?>> getHttpMessageConverters() {

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        AllEncompassingFormHttpMessageConverter converter = new AllEncompassingFormHttpMessageConverter();
        messageConverters.add(converter);

        // 메시지 컨버터 지정(특히 POST 전송시 한글 깨지는 문제 해결)
        FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        List<HttpMessageConverter<?>> partConverters = new ArrayList<>();
        partConverters.add(new ByteArrayHttpMessageConverter());
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        stringHttpMessageConverter.setWriteAcceptCharset(false);
        partConverters.add(stringHttpMessageConverter);
        partConverters.add(new ResourceHttpMessageConverter());
        formHttpMessageConverter.setPartConverters(partConverters);
        messageConverters.add(formHttpMessageConverter);
        stringHttpMessageConverter.setWriteAcceptCharset(true);
        messageConverters.add(stringHttpMessageConverter);

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setDateFormat(new SimpleDateFormat(ISO_DATE_FORMAT));
        jsonConverter.setObjectMapper(objectMapper);
        messageConverters.add(jsonConverter);

        return messageConverters;
    }

    private CloudFlareService(CFBuilder cfBuilder){
        this.X_AUTH_KEY = cfBuilder.X_AUTH_KEY;
        this.X_AUTH_EMAIL = cfBuilder.X_AUTH_EMAIL;
        this.ACCOUNT_UID = cfBuilder.ACCOUNT_UID;
        this.API_TOKEN = cfBuilder.API_TOKEN;
    }

    public static class CFBuilder {
        private String X_AUTH_KEY;
        private String X_AUTH_EMAIL;
        private String ACCOUNT_UID;
        private String API_TOKEN;

        public CFBuilder(String xAuthKey, String xAuthEmail, String accountUid, String apiToken){
            this.X_AUTH_KEY = xAuthKey;
            this.X_AUTH_EMAIL = xAuthEmail;
            this.ACCOUNT_UID = accountUid;
            this.API_TOKEN = apiToken;
        }

        public CloudFlareService build(){
            return new CloudFlareService(this);
        }
    }

    public CFImageResult uploadImage(MultipartFile file){
        RestTemplate restTemplate = new RestTemplate(getHttpRequestFactory());
        restTemplate.setMessageConverters(getHttpMessageConverters());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        ByteArrayResource resource = null;
        try {
            resource = new ByteArrayResource(file.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.setBearerAuth(API_TOKEN);
        HttpEntity formEntity = new HttpEntity<>(parameters, headers);

        ResponseEntity<LinkedHashMap> response = restTemplate.exchange("https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_UID + "/images/v1", HttpMethod.POST, formEntity, LinkedHashMap.class);
        LinkedHashMap body = Utils.toObject(response.getBody(), LinkedHashMap.class);
        LinkedHashMap result = Utils.toObject(response.getBody().get("result"), LinkedHashMap.class);
        boolean success = Boolean.valueOf(body.get("success").toString());
        CFImageResult cfImageResult = new CFImageResult();
        if(success){
            String uid = result.get("id").toString();
            String variants = result.get("variants").toString();
            List<String> videoViewUrls = new ArrayList<>();
            if(variants != null){
                String[] variantArr = variants.replace("[", "").replace("]", "").replace("\"", "").replace(" ", "").split(",");
                for(String variant: variantArr){
                    videoViewUrls.add(variant);
                }
                if(!videoViewUrls.isEmpty()){
                    cfImageResult.setImageUrl(videoViewUrls);
                }
            }
            cfImageResult.setSuccess(true);
            cfImageResult.setUid(uid);
        } else {
            cfImageResult.setSuccess(false);
        }

        return cfImageResult;

    }

    public CFVideoResult uploadStreamVideo(String url, String fileName){
        RestTemplate restTemplate = new RestTemplate(getHttpRequestFactory());
        restTemplate.setMessageConverters(getHttpMessageConverters());
        String bodyString = "{\"url\":\""+url + "\",\"meta\":{\"name\":\"" + fileName + "\"}}";

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Auth-Key", X_AUTH_KEY);
        headers.add("X-Auth-Email", X_AUTH_EMAIL);
        HttpEntity formEntity = new HttpEntity<>(bodyString, headers);
        final ResponseEntity<LinkedHashMap> response = restTemplate.exchange("https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_UID + "/stream/copy", HttpMethod.POST, formEntity, LinkedHashMap.class);
        LinkedHashMap body = Utils.toObject(response.getBody(), LinkedHashMap.class);
        LinkedHashMap result = Utils.toObject(response.getBody().get("result"), LinkedHashMap.class);
        boolean success = Boolean.valueOf(body.get("success").toString());
        CFVideoResult cfVideoResult = new CFVideoResult();
        if(success){
            String uid = result.get("uid").toString();
            String preview = result.get("preview").toString();
            cfVideoResult.setSuccess(true);
            cfVideoResult.setUid(uid);
            cfVideoResult.setPreview(preview);
        } else {
            cfVideoResult.setSuccess(false);
        }

        return cfVideoResult;
    }

}

