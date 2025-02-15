package org.dromara.daxpay.unisdk.common.http;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.daxpay.unisdk.common.bean.result.PayException;
import org.dromara.daxpay.unisdk.common.exception.PayErrorException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * URL表达式处理器
 *
 * @author egan
 * <pre>
 * email egzosn@gmail.com
 * date 2017/3/5 10:07
 * </pre>
 */
@Slf4j
public final class UriVariables {
    public static final String QUESTION = "?";

    private UriVariables() {
    }

    /**
     * 依次匹配
     *
     * @param uri          匹配的uri，带代表式
     * @param uriVariables 匹配表达式的值
     * @return 匹配完的url
     * <code>
     * System.out.println(getUri(&quot;http://egan.in/{a}/ba/{a1}?{bb}={a1}&quot;, &quot;no1&quot;, &quot;no2&quot;, &quot;no3&quot;, &quot;no4&quot;));
     * 结果 http://egan.in/no1/ba/no2?no3=no4
     * </code>
     */
    public static String getUri(String uri, Object... uriVariables) {

        if (null == uriVariables) {
            return uri;
        }
        for (Object variable : uriVariables) {
            if (null == variable) {
                continue;
            }
            uri = uri.replaceFirst("\\{\\w+\\}", variable.toString());
        }
        return uri;
    }


    /**
     * 匹配Map.key
     *
     * @param uri          匹配的uri，带代表式
     * @param uriVariables 匹配表达式的值
     * @return 匹配完的url
     * <code>
     * Map&lt;String, Object&gt;  uriVariable = new HashMap&lt;String, Object&gt;();
     * uriVariable.put(&quot;a&quot;, &quot;no1&quot;);
     * uriVariable.put(&quot;a1&quot;, &quot;no2&quot;);
     * uriVariable.put(&quot;bb&quot;, &quot;no3&quot;);
     * System.out.println(getUri(&quot;http://egan.in/{a}/ba/{a1}?{bb}={a1}&quot;, uriVariable));
     * 结果 http://egan.in/no1/ba/no2?no3=no2
     * </code>
     */
    public static String getUri(String uri, Map<String, Object> uriVariables) {

        if (null == uriVariables) {
            return uri;
        }
        for (Map.Entry<String, Object> entry : uriVariables.entrySet()) {
            Object uriVariable = entry.getValue();
            if (null == uriVariable) {
                continue;
            }

            uri = uri.replace("{" + entry.getKey() + "}", uriVariable.toString());
        }
        return uri;
    }


    /**
     * Map转化为对应得参数字符串
     *
     * @param pe 参数
     * @return 参数字符串
     */
    public static String getMapToParameters(Map<String, ?> pe) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ?> entry : pe.entrySet()) {
            Object o = entry.getValue();

            if (null == o) {
                continue;
            }

            if (o instanceof List) {
                o = ((List) o).toArray();
            }
            if (o instanceof Object[] os) {
                StringBuilder valueStr = new StringBuilder();
                for (int i = 0, len = os.length; i < len; i++) {
                    if (null == os[i]) {
                        continue;
                    }
                    String value = os[i].toString().trim();
                    valueStr.append((i == len - 1) ? value : value + ",");
                }
                builder.append(entry.getKey()).append("=").append(urlEncoder(valueStr.toString())).append("&");
                continue;
            }
            builder.append(entry.getKey()).append("=").append(urlEncoder(entry.getValue().toString())).append("&");

        }
        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * 解析应答字符串，生成应答要素
     *
     * @param str 需要解析的字符串
     * @return 解析的结果map
     */
    public static JSONObject getParametersToMap(String str) {

        JSONObject map = new JSONObject();
        int len = str.length();
        StringBuilder temp = new StringBuilder();
        char curChar;
        String key = null;
        boolean isKey = true;
        boolean isOpen = false;//值里有嵌套
        char openName = 0;
        if (len > 0) {
            // 遍历整个带解析的字符串
            for (int i = 0; i < len; i++) {
                curChar = str.charAt(i);// 取当前字符
                if (isKey) {// 如果当前生成的是key

                    if (curChar == '=') {// 如果读取到=分隔符
                        key = temp.toString();
                        temp.setLength(0);
                        isKey = false;
                    }
                    else {
                        temp.append(curChar);
                    }
                }
                else {// 如果当前生成的是value
                    if (isOpen) {
                        if (curChar == openName) {
                            isOpen = false;
                        }

                    }
                    else {//如果没开启嵌套
                        if (curChar == '{') {//如果碰到，就开启嵌套
                            isOpen = true;
                            openName = '}';
                        }
                        if (curChar == '[') {
                            isOpen = true;
                            openName = ']';
                        }
                    }
                    if (curChar == '&' && !isOpen) {// 如果读取到&分割符,同时这个分割符不是值域，这时将map里添加
                        putKeyValueToMap(temp, false, key, map);
                        temp.setLength(0);
                        isKey = true;
                    }
                    else {
                        temp.append(curChar);
                    }
                }

            }
            putKeyValueToMap(temp, isKey, key, map);
        }
        return map;
    }

    private static void putKeyValueToMap(StringBuilder temp, boolean isKey, String key, Map<String, Object> map) {
        if (isKey) {
            key = temp.toString();
            if (key.isEmpty()) {
                throw new PayErrorException(new PayException("QString format illegal", "内容格式有误"));
            }
            map.put(key, "");
        }
        else {
            if (key.isEmpty()) {
                throw new PayErrorException(new PayException("QString format illegal", "内容格式有误"));
            }
            map.put(key, temp.toString());
        }
    }

    public static String urlEncoder(String str) {
        return urlEncoder(str, "utf-8");
    }

    public static String urlEncoder(String str, String enc) {
        try {
            return URLEncoder.encode(str, enc);
        }
        catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return str;
    }

    /**
     * 去除域名的标准url
     *
     * @param url url
     * @return 去除域名的标准url
     */
    public static String getCanonicalUrl(String url) {
        if (StrUtil.isEmpty(url)) {
            return url;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            String encodedQuery = uri.getQuery();
            if (StrUtil.isNotEmpty(encodedQuery)) {
                path += QUESTION.concat(encodedQuery);
            }
            return path;
        }
        catch (URISyntaxException e) {
            throw new PayErrorException(new PayException("failure", "去除域名的标准url失败"), e);
        }

    }
}
