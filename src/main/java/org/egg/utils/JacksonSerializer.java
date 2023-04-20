package org.egg.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class JacksonSerializer {

    public static String serialize(Object data) {
        if (data == null) return null;
        if (data instanceof String) return (String) data;
        try {
            return mapper.writeValueAsString(data);
        } catch (IOException e) {
            logger.error("serialize data error", e);
            return null;
        }
    }

    public static <T> T deSerialize(String content, Class<T> clazz) {
        return deSerialize(content, clazz, null, false);
    }

    /**
     * 反序列化过程中出现的IO异常会被包装成RuntimeException抛出
     */
    public static <T> T deSerializeThrowEx(String content, Class<T> clazz) {
        return deSerialize(content, clazz, null, true);
    }

    public static <T> T deSerialize(String content, TypeReference<T> t) {
        return deSerialize(content, null, t, false);
    }

    /**
     * 反序列化过程中出现的IO异常会被包装成RuntimeException抛出
     */
    public static <T> T deSerializeThrowEx(String content, TypeReference<T> t) {
        return deSerialize(content, null, t, true);
    }

    /**
     * 反序列化当前节点下的某一字段值
     * 目前不支持序列化二级节点及以上节点的字段值
     *
     * @param content   Json 串
     * @param fieldName 一级节点下的字段值
     * @param clazz     反序列化字段类型
     * @return 参数非法和IOEx异常时返回null
     */
    public static <T> T deSerializeField(String content, String fieldName, Class<T> clazz) {
        if (StringUtils.isBlank(content) || StringUtils.isBlank(fieldName) || Objects.isNull(clazz)) {
            return null;
        }

        try {
            JsonNode nodeValue = fetchFieldNode(content, fieldName);

            return mapper.treeToValue(nodeValue, clazz);
        } catch (IOException e) {
            logger.error("deserialize node object error, content={}, fieldName={}, clazz={} :", content, fieldName, clazz, e);
            return null;
        }
    }

    /**
     * 反序列化当前节点下的某一字段值
     * 目前不支持序列化二级节点及以上节点的字段值
     *
     * @param content   Json 串
     * @param fieldName 一级节点下的字段值
     * @param t         反序列化字段类型
     * @return 参数非法和IOEx异常时返回null
     */
    public static <T> T deSerializeField(String content, String fieldName, TypeReference<T> t) {
        if (StringUtils.isBlank(content) || StringUtils.isBlank(fieldName) || Objects.isNull(t)) {
            return null;
        }

        try {
            JsonNode nodeValue = fetchFieldNode(content, fieldName);

            return deSerialize(nodeValue.toString(), t);
        } catch (IOException e) {
            logger.error("deserialize node object error, content={}, fieldName={}, t={} :", content, fieldName, t, e);
            return null;
        }
    }

    /**
     * 将Json String转换为Map参数
     * 反序列化过程中出现的IO异常会被包装成RuntimeException抛出
     */
    public static Map<String, String> jsonToMap(String jsonStr) {
        return jsonToMap(jsonStr, false);
    }

    /**
     * 将Json String转换为Map参数
     */
    public static Map<String, String> jsonToMapThrowEx(String jsonStr) {
        return jsonToMap(jsonStr, true);
    }

    private static JsonNode fetchFieldNode(String content, String fieldName) throws IOException {
        JsonNode treeNode = mapper.readTree(content);
        return treeNode.get(fieldName);
    }

    private static Map<String, String> jsonToMap(String jsonStr, boolean throwEx) {
        Map<String, String> resultMap = Maps.newHashMap();
        if (Strings.isNullOrEmpty(jsonStr)) {
            return resultMap;
        }

        try {
            Map<String, Object> tempMap = mapper.readValue(jsonStr, Map.class);
            for (Map.Entry<String, Object> entry : tempMap.entrySet()) {
                resultMap.put(entry.getKey(), serialize(entry.getValue()));
            }
        } catch (IOException e) {
            logger.error("jsonToMap error: {}", jsonStr, e);
            if (throwEx) throw new RuntimeException(e);
        }
        return resultMap;
    }

    /**
     * 定制module
     *
     * @param module 序列化器
     */
    public static void registerModule(SimpleModule module) {
        mapper.registerModule(module);
    }

    private static <T> T deSerialize(String content, Class<T> clazz, TypeReference<T> t, boolean throwEx) {
        if (content == null) {
            return null;
        }

        if (clazz == null && t == null) {
            return null;
        }

        if (clazz != null && Objects.equals(String.class, clazz)) {
            return (T) content;
        }

        if (content.length() == 0) {
            return null;
        }

        if (t != null && TypeUtils.isAssignable(content.getClass(), t.getType())) {
            return (T) content;
        }

        try {
            if (clazz != null) {
                return mapper.readValue(content, clazz);
            } else {
                return mapper.readValue(content, t);
            }
        } catch (IOException e) {
            logger.error("deserialize object error: {}", content, e);
            if (throwEx) throw new RuntimeException(e);
            return null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(JacksonSerializer.class);

    protected static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.disable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

}
