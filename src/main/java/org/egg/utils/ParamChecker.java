package org.egg.utils;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * @author tongjie Initial Created at 21/07/2017
 * @since 202112 support format <pre> ParamChecker.notBlank("", "Db Object id={} field is blank!", 3);</per>
 */
public final class ParamChecker {

    // ===============public static methods====================
    public static void notBlank(String arg, String errMsg) {
        checkArgument(StringUtils.isNotBlank(arg), errMsg);
    }

    /**
     * 相比于 {@link #notBlank(String, String) 之前的}
     * 使用和Slf4j日志格式化一致的方案来避免多余的字符串连接，
     * 并且简化代码。
     * <p/>
     * e.g. <pre> ParamChecker.notBlank("", "Db Object id={} name={} field is blank!", 3, "zhangSan");</per>
     */
    public static void notBlank(String arg, String errMsgFormat, Object... errMsgArg) {
        if (!StringUtils.isNotBlank(arg)) {
            formatAndThrowLae(errMsgFormat, errMsgArg);
        }
    }

    public static void notNull(Object arg, String errMsg) {
        checkArgument(arg != null, errMsg);
    }

    public static void checkArgument(boolean condition, String msg){
        if (!condition){
            throw new IllegalArgumentException(msg);
        }
    }

    public static void notNull(Object arg, String errMsgFormat, Object... errMsgArg) {
        if (null == arg) {
            formatAndThrowLae(errMsgFormat, errMsgArg);
        }
    }

    public static void isTrue(boolean flag, String errMsg) {
        checkArgument(flag, errMsg);
    }

    public static void isTrue(boolean flag, String errMsgFormat, Object... errMsgArg) {
        if (!flag) {
            formatAndThrowLae(errMsgFormat, errMsgArg);
        }
    }

    public static void notEmpty(Collection<?> collection, String errMsg) {
        checkArgument(collection != null && collection.size() > 0, errMsg);
    }

    public static void notEmpty(Collection<?> collection, String errMsgFormat, Object... errMsgArg) {
        if (null == collection || collection.size() <= 0) {
            formatAndThrowLae(errMsgFormat, errMsgArg);
        }
    }

    public static void notEmpty(Map<?, ?> map, String errMsg) {
        checkArgument(map != null && map.size() > 0, errMsg);
    }

    public static void notEmpty(Map<?, ?> map, String errMsgFormat, Object... errMsgArg) {
        if (null == map || map.size() <= 0) {
            formatAndThrowLae(errMsgFormat, errMsgArg);
        }
    }

    public static void checkMaxLength(String arg, int maxLength, String errMsg) {
        if (arg == null) {
            return;
        }
        checkArgument(arg.length() <= maxLength, errMsg);
    }

    public static void checkMinLength(String arg, int minLength, String errMsg) {
        if (arg == null) {
            return;
        }
        checkArgument(arg.length() >= minLength, errMsg);
    }

    /**
     * 金额非法，金额非空或者金额大于0前
     */
    public static void checkAmt(BigDecimal amt, String message) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 使用和Slf4j日志格式化一致方案
     *
     * @throws IllegalArgumentException 抛出参数异常
     */
    private static void formatAndThrowLae(String errMsgFormat, Object... errMsgArg) {
        String errMsg = MessageFormatter.arrayFormat(errMsgFormat, errMsgArg).getMessage();
        throw new IllegalArgumentException(String.valueOf(errMsg));
    }

    private ParamChecker() {
    }

}
