package com.xxblue.mcf.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ExchangeRestTemplate {
    
    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    private RestTemplate restTemplate;
    
    private int connectionTimeout = 5000;  // default: 10 sec.
    
    private int readTimeout = 10000;        // default: 20 sec.
    
    private boolean latencyMonitorEnable = true;        // default: true
    
    public ExchangeRestTemplate() {
        restTemplate = new RestTemplate(getHttpRequestFactory());
        restTemplate.setMessageConverters(getHttpMessageConverters());
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

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
    
}