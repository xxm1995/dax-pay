package cn.bootx.platform.daxpay.sdk.util;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONUtil;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 如果需要进行签名,
 *  1. 参数名ASCII码从小到大排序（字典序）
 *  2. 如果参数的值为空不参与签名
 *  3. 参数名不区分大小写
 *  4. 嵌套对象转换成先转换成MAP再序列化为字符串
 *  5. 支持两层嵌套, 更多层级嵌套未测试, 可能会导致不可预知的问题
 */
@UtilityClass
public class PaySignUtil {

    /**
     * 将参数转换为map对象. 使用ChatGPT生成
     * 1. 参数名ASCII码从小到大排序（字典序）
     * 2. 如果参数的值为空不参与签名；
     * 3. 参数名不区分大小写；
     */
    public Map<String, String> toMap(Object object) {
        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        toMap(object, map);
        return map;
    }

    /**
     * 将参数转换为map对象. 使用ChatGPT生成, 仅局限于对请求支付相关参数进行签名
     */
    @SneakyThrows
    private void toMap(Object object, Map<String, String> map) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldValue = field.get(object);
                if (fieldValue != null) {
                    // 基础类型及包装类 和 字符串类型
                    if (ClassUtil.isBasicType(field.getType())|| field.getType().equals(String.class)) {
                        String fieldValueString = String.valueOf(fieldValue);
                        map.put(fieldName, fieldValueString);
                    }
                    // 集合类型
                    else if (Collection.class.isAssignableFrom(field.getType())) {
                        Collection<?> collection = (Collection<?>) fieldValue;
                        if (!collection.isEmpty()) {
                            List<Map<String, String>> maps = collection.stream()
                                    .filter(Objects::nonNull)
                                    .map(item -> {
                                        Map<String, String> nestedMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                                        toMap(item, nestedMap);
                                        return nestedMap;
                                    })
                                    .collect(Collectors.toList());
                            map.put(fieldName,  JSONUtil.toJsonStr(maps));
                        }
                        // 其他类型
                    } else {
                        Map<String, String> nestedMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                        toMap(fieldValue, nestedMap);
                        String nestedJson = JSONUtil.toJsonStr(map);
                        map.put(fieldName, nestedJson);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }


    /**
     * 把所有元素排序, 并拼接成字符, 用于签名
     */
    public static String createLinkString(Map<String, String> params) {
        String connStr = "&";
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            // 拼接时，不包括最后一个&字符
            if (i == keys.size() - 1) {
                content.append(key)
                        .append("=")
                        .append(value);
            } else {
                content.append(key)
                        .append("=")
                        .append(value)
                        .append(connStr);
            }
        }
        return content.toString();
    }


    /**
     * 生成16进制 MD5 字符串
     *
     * @param data 数据
     * @return MD5 字符串
     */
    public String md5(String data) {
        return SecureUtil.md5(data);
    }

    /**
     * 生成16进制的 sha256 字符串
     *
     * @param data 数据
     * @param signKey  密钥
     * @return sha256 字符串
     */
    public String hmacSha256(String data, String signKey) {
        return SecureUtil.hmac(HmacAlgorithm.HmacSHA256, signKey).digestHex(data);
    }

    /**
     * 生成待签名字符串
     * @param object 待签名对象
     * @param signKey 签名Key
     * @return 待签名字符串
     */
    public String signString(Object object, String signKey){
        // 签名
        Map<String, String> map = toMap(object);
        // 生成签名前先去除sign参数
        map.remove("sign");
        // 创建待签名字符串
        String data = createLinkString(map);
        // 将签名key追加到字符串最后
        return data +  "&key=" + signKey;
    }

    /**
     * md5方式进行签名
     *
     * @return 签名值
     */
    public String md5Sign(Object object, String signKey){
        String data = signString(object, signKey);
        return md5(data);
    }

    /**
     * hmacSha256方式进行签名
     *
     * @return 签名值
     */
    public String hmacSha256Sign(Object object, String signKey){
        String data = signString(object, signKey);
        return hmacSha256(data, signKey);
    }

    /**
     * MD5签名验证
     */
    public boolean verifyMd5Sign(Object object, String signKey, String sign){
        String md5Sign = md5Sign(object, signKey);
        return md5Sign.equals(sign);
    }

    /**
     * hmacSha256签名验证
     */
    public boolean verifyHmacSha256Sign(Object object, String signKey, String sign){
        String hmacSha256Sign = hmacSha256Sign(object, signKey);
        return hmacSha256Sign.equals(sign);
    }

}
