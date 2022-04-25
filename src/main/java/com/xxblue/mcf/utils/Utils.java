package com.xxblue.mcf.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;

@Slf4j
public class Utils {

    public static BigDecimal SATOSHI = new BigDecimal("0.00000001");

    private static ObjectMapper mObjectMapper = null;

    public static ObjectMapper getObjectMapper() {
        if (mObjectMapper == null) {
            mObjectMapper = new ObjectMapper();
            mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mObjectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mObjectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            mObjectMapper.registerModule(new JavaTimeModule());
            mObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return mObjectMapper;
    }

    /**
     * 주어진 객체가 null이거나 값이 비어있는지 검사한다.
     * http://stove99.tistory.com/73 참조
     * @param obj
     * @return
     */
    public static boolean isEmpty(Object obj) {

        if (obj instanceof String) {
            return obj == null || "".equals(obj.toString().trim());
        } else if (obj instanceof List) {
            return obj == null || ((List<?>) obj).isEmpty();
        } else if (obj instanceof Map) {
            return obj == null || ((Map<?,?>) obj).isEmpty();
        } else if (obj instanceof Object[]) {
            return obj == null || Array.getLength(obj) == 0;
        } else {
            return obj == null;
        }
    }

    public static boolean isNotEmpty(String s) {

        return !isEmpty(s);
    }

    /**
     * MessageFormat 클래스를 사용하여 지정된 메지시를 형식화 한다.
     * @param format
     * @param args
     * @return
     */
    public static String fm(String format, Object...args) {
        return MessageFormat.format(format, args);
    }

    /**
     * 이메일 ID에서 '.'과 '+' 문자를 제거한다.
     *
     * @param email
     * @return
     */
    public static String normalizeEmail(String email) {

        String user = email.split("@")[0];
        String domain = email.substring(email.indexOf('@'));
        user = user.replaceAll("\\.", "");
        if (user.contains("+")) {
            user = user.substring(0, user.indexOf("+"));
        }
        return user + domain;
    }

    /**
     * 주어진 문자열이 유효한 JSON 문자열인지 알려준다.<br>
     * <a href="http://stackoverflow.com/questions/10174898/how-to-check-whether-a-given-string-is-valid-json-in-java">
     *     stackoverflow : how-to-check-whether-a-given-string-is-valid-json-in-java</a>
     *
     * @param json
     * @return
     */
    public static boolean isValidJSON(final String json) {

        try {
            final ObjectMapper om = new ObjectMapper();
            om.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String toJson(Object object) {

        ObjectMapper objectMapper = getObjectMapper();
        String json = "";
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static String toJsonPretty(Object object) {

        ObjectMapper objectMapper = getObjectMapper();
        String json = "";
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static Map<String, String> toMapFromJson(String json) {

        ObjectMapper objectMapper = getObjectMapper();
        Map<String, String> ret = new LinkedHashMap<>();
        try {
            ret = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * ARS 또는 이메일에서 사용할 인증 코드를 발생시킨다.
     * @return
     */
    public static final String generateOtp() {

        return generateOtp(6);
    }

    public static final String generateOtp(int length) {

        StringBuffer sb = new StringBuffer();
        int x = 0;
        while (x == 0) {
            x = (int) (Math.random() * 10); // 0이 아닌 숫자 발생
        }
        sb.append(x);
        for (int i = 0; i < length-1; i++) {
            x = (int) (Math.random() * 10);
            sb.append(x);
        }
        return sb.toString();
    }

    public static <T> T toObject(Object object, Class<T> clazz) {

        ObjectMapper objectMapper = getObjectMapper();
        T obj = null;
        try {
            obj = objectMapper.readValue(toJson(object), clazz);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return obj;
    }

    public static <T> T toObject(String json, Class<T> clazz) {

        ObjectMapper objectMapper = getObjectMapper();
        T obj = null;
        try {
            obj = objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return obj;
    }

    public static <T> T convertValue(Object object, TypeReference<T> clazz) {

        ObjectMapper objectMapper = getObjectMapper();
        T obj = objectMapper.convertValue(object, clazz);
        return obj;
    }

    /**
     * 자바 오브젝트를 맵으로 변환<br>
     * <a href="http://erictus.tistory.com/entry/Map-to-Object-%EC%99%80-Object-to-Map">http://erictus.tistory.com/entry/Map-to-Object-%EC%99%80-Object-to-Map</a>
     * @param obj
     * @return
     */
    public static Map<String, Object> toMap(Object obj) {

        Map<String, Object> result = new HashMap<>();
        try {
            BeanInfo info = Introspector.getBeanInfo(obj.getClass());
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                Method reader = pd.getReadMethod();
                if (reader != null) {
                    result.put(pd.getName(), reader.invoke(obj));
                }
            }
        } catch (Exception e) {
            result.put(obj.getClass().getName(), obj);
        }
        return result;
    }

    /**
     * 테이블에 문자열 형태로 저장되는 사용자 로케일을 받아 java {@link Locale} 객체로 변환한다.
     * null 또는 해석할 수 없는 문자열일 경우 {@link Locale#getDefault()}를 리턴한다.
     * <p>
     * 예1 : "ko" -> Locale("ko")
     * 예2 : "en" -> Locale("en")
     * 예3 : "zh_cn" -> Locale("zh", "CN")
     * 예4 : "_KR" -> Locale("", "KR")
     *
     * @param localeStr
     * @return
     */
    public static Locale getLocale(String localeStr) {

        if (localeStr == null) {
            log.debug("[invalid] use default : " + localeStr);
            return Locale.getDefault();
        }
        // validate locale
        Locale locale = parseLocale(localeStr);
        if (isValid(locale)) {
            log.trace(locale.getDisplayName() + "[" + locale.toString() + "]");
        } else {
            locale = Locale.getDefault();
            log.debug("[invalid] use default : " + localeStr);
        }
        return locale;
    }

    /**
     * 로케일 문자열을 분석하여 해당 Locale 객체를 반환한다.
     * @param localeStr
     * @return
     */
    private static Locale parseLocale(String localeStr) {

        String[] parts = localeStr.split("_");
        switch (parts.length) {
            case 3: return new Locale(parts[0], parts[1], parts[2]);
            case 2: return new Locale(parts[0], parts[1]);
            case 1: return new Locale(parts[0]);
            default: throw new IllegalArgumentException("Invalid locale: " + localeStr);
        }
    }

    /**
     * 유효한 Locale 객체인지 확인한다.
     * @param locale
     * @return
     */
    private static boolean isValid(Locale locale) {
        try {
            return locale.getISO3Language() != null && locale.getISO3Country() != null;
        } catch (MissingResourceException e) {
            return false;
        }
    }


    public static String toKrw(BigDecimal amount) {
        if(amount == null) {
            return "0";
        }
        return new DecimalFormat("#,##0").format(amount.doubleValue());
    }

    public static String toUsd(BigDecimal amount) {
        return new DecimalFormat("#,##0.00").format(amount.doubleValue());
    }

    public static String toBtc(BigDecimal amount) {
        if(amount == null){
            return null;
        }
        return new DecimalFormat("#,##0.########").format(amount.doubleValue());
    }

    /**
     * 매도 수수료를 계산한다. (quote currency 단위)
     * 매도한 코인 수량과 호가를 곱하여 합계 금액을 계산 후 수수료를 계산한다.
     * 수수료 계산 시 소수이하 첫째 자리에서 올림 처리한다.
     *
     * @param size 매도량
     * @param price 호가
     * @param feeRate 수수료율
     * @return
     */
    public static BigDecimal calcQuoteFee(BigDecimal size, BigDecimal price, BigDecimal feeRate) {

        BigDecimal subtotal = size.multiply(price).setScale(0, BigDecimal.ROUND_CEILING); // 주문 금액
        return calcKrwFee(subtotal, feeRate);
    }

    public static BigDecimal calcKrwFee(BigDecimal subtotal, BigDecimal feeRate) {

        return subtotal.multiply(feeRate.divide(new BigDecimal("100"))).setScale(0, BigDecimal.ROUND_CEILING); // 수수료;
    }

    /**
     * 매수 수수료를 계산한다. (base currency 단위)
     * 매수한 코인 수량에 수수료율을 적용하여 수수료를 계산한다.
     * 수수료가 최소 단위 미만인 경우 최소 단위를 적용한다.
     *
     * @param amount 매수량
     * @param feeRate 수수료율
     * @return
     */
    public static BigDecimal calcCoinFee(BigDecimal amount, BigDecimal feeRate) {

        BigDecimal fee = amount.multiply(feeRate.divide(new BigDecimal("100"))).setScale(8, BigDecimal.ROUND_HALF_UP); // 수수료;
        if (fee.compareTo(SATOSHI) < 0) {
            return SATOSHI;
        } else {
            return fee;
        }
    }

    /**
     * class 에 @RestController 가 있거나 method 에 @ResponseBody 가 있을 경우 rest api 로 판단.
     */
    public static Boolean isRestApi(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(ResponseBody.class)
                || handlerMethod.getMethod().getDeclaringClass().isAnnotationPresent(RestController.class);
    }

    public static String makeRandomNumber(int len, int dupCd){
        Random rand = new Random();
        String numStr = ""; //난수가 저장될 변수

        for(int i=0;i<len;i++) {

            //0~9 까지 난수 생성
            String ran = Integer.toString(rand.nextInt(10));

            if(dupCd==1) {
                //중복 허용시 numStr에 append
                numStr += ran;
            }else if(dupCd==2) {
                //중복을 허용하지 않을시 중복된 값이 있는지 검사한다
                if(!numStr.contains(ran)) {
                    //중복된 값이 없으면 numStr에 append
                    numStr += ran;
                }else {
                    //생성된 난수가 중복되면 루틴을 다시 실행한다
                    i-=1;
                }
            }
        }
        return numStr;
    }

    public static String uuid(){
        return UUID.randomUUID().toString();
    }

}