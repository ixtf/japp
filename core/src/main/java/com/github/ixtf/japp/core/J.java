package com.github.ixtf.japp.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ixtf.japp.core.cli.SaferExec;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.ixtf.japp.core.Constant.MAPPER;

public class J {

    @SneakyThrows
    public static <T> Class<T> actualClass(Class<T> clazz) {
        final String className = clazz.getName();
        if (J.contains(className, "CGLIB")) {
            final String actualClassName = J.split(className, "$$")[0];
            return (Class<T>) Class.forName(actualClassName);
        }
        return clazz;
    }

    public static String strTpl(final String tpl, Map<String, String> map) {
        return StringSubstitutor.replace(tpl, map);
    }

    public static String exeCli(final String command) {
        SaferExec exec = new SaferExec();
        return exec.exec(command);
    }

    public static LocalDateTime localDateTime(Date date) {
        return Optional.ofNullable(date)
                .map(Date::toInstant)
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElseThrow(() -> new NullPointerException());
    }

    public static LocalDate localDate(Date date) {
        return Optional.ofNullable(date)
                .map(J::localDateTime)
                .map(LocalDateTime::toLocalDate)
                .orElseThrow(() -> new NullPointerException());
    }

    public static LocalTime localTime(Date date) {
        return Optional.ofNullable(date)
                .map(J::localDateTime)
                .map(LocalDateTime::toLocalTime)
                .orElseThrow(() -> new NullPointerException());
    }

    public static Date date(LocalDateTime ldt) {
        return Optional.ofNullable(ldt)
                .map(it -> it.atZone(ZoneId.systemDefault()))
                .map(ZonedDateTime::toInstant)
                .map(Date::from)
                .orElseThrow(() -> new NullPointerException());
    }

    public static Date date(LocalDate ld, LocalTime lt) {
        return Optional.ofNullable(ld)
                .flatMap(__ -> Optional.ofNullable(lt)
                        .map(___ -> ld.atTime(lt))
                )
                .map(J::date)
                .orElseThrow(() -> new NullPointerException());
    }

    public static Date date(LocalDate ld) {
        return Optional.ofNullable(ld)
                .map(LocalDate::atStartOfDay)
                .map(J::date)
                .orElseThrow(() -> new NullPointerException());
    }

    public static Date date(LocalTime lt) {
        return Optional.ofNullable(lt)
                .map(it -> it.atDate(LocalDate.now()))
                .map(J::date)
                .orElseThrow(() -> new NullPointerException());
    }

    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    public static <E> ArrayList<E> newArrayList(E... elements) {
        int capacity = 5 + elements.length + elements.length / 10;
        ArrayList<E> list = new ArrayList<>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    public static <K, V> Map<K, V> newHashMap() {
        return new HashMap<>();
    }

    public static <K, V> Map<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    public static boolean deleteQuietly(File file) {
        return FileUtils.deleteQuietly(file);
    }

    public static void forceDelete(File file) throws IOException {
        FileUtils.forceDelete(file);
    }

    public static Collection<File> listFiles(final String directory, final String[] extensions, final boolean recursive) {
        return FileUtils.listFiles(getFile(directory), extensions, recursive);
    }

    public static Collection<File> listFiles(final File directory, final String[] extensions, final boolean recursive) {
        return FileUtils.listFiles(directory, extensions, recursive);
    }

    public static File getFile(String... names) {
        return FileUtils.getFile(names);
    }

    public static File getFile(File directory, String... names) {
        return FileUtils.getFile(directory, names);
    }

    public static File getTempDirectory() {
        return FileUtils.getTempDirectory();
    }

    public static void moveFile(File srcFile, File destFile) throws IOException {
        FileUtils.moveFile(srcFile, destFile);
    }

    public static String toJson(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        return Optional.ofNullable(o)
                .map(it -> {
                    try {
                        return MAPPER.writeValueAsString(it);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(null);
    }

    /**
     * @param o 需要转换的对象
     * @return null 变为 {@link NullNode};{@link String} 变为 {@link ObjectMapper#readTree};{@link Collection} 变为 {@link ArrayNode}
     */
    public static JsonNode toJsonNode(Object o) {
        try {
            if (o == null) {
                return NullNode.instance;
            } else if (o instanceof String) {
                String s = (String) o;
                return MAPPER.readTree(s);
            } else if (o.getClass().isPrimitive()) {
                throw new IllegalArgumentException();
            } else if (o instanceof JsonNode) {
                return (JsonNode) o;
            } else if (o instanceof Collection) {
                return toArrayNode((Collection) o);
            }
            return MAPPER.getNodeFactory().pojoNode(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectNode toObjectNode(Object o) {
        return Optional.ofNullable(o)
                .map(it -> MAPPER.convertValue(it, ObjectNode.class))
                .get();
    }

    public static ArrayNode toArrayNode(Collection c) {
        ArrayNode arrayNode = MAPPER.createArrayNode();
        c.stream().forEach(it -> arrayNode.add(J.toJsonNode(it)));
        return arrayNode;
    }

    public static boolean isBaseDataType(Object o) {
        return isBaseDataType(o.getClass());
    }

    public static boolean isBaseDataType(Class clazz) {
        return CharSequence.class.isAssignableFrom(clazz) ||
                Number.class.isAssignableFrom(clazz) ||
                Date.class.isAssignableFrom(clazz) ||
                TemporalAccessor.class.isAssignableFrom(clazz) ||
                clazz.equals(Character.class) ||
                clazz.equals(Boolean.class) ||
                clazz.isPrimitive();
    }

    public static boolean isBlank(CharSequence cs) {
        return StringUtils.isBlank(cs);
    }

    public static boolean nonBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static String deleteWhitespace(String s) {
        return StringUtils.deleteWhitespace(s);
    }

    public static String[] split(String s) {
        return StringUtils.split(s);
    }

    public static int length(CharSequence cs) {
        return StringUtils.length(cs);
    }

    public static boolean contains(final CharSequence seq, final CharSequence searchSeq) {
        return StringUtils.contains(seq, searchSeq);
    }

    public static boolean nonContains(final CharSequence seq, final CharSequence searchSeq) {
        return !contains(seq, searchSeq);
    }

    public static boolean contains(final CharSequence seq, final int searchChar) {
        return StringUtils.contains(seq, searchChar);
    }

    public static boolean nonContains(final CharSequence seq, final int searchChar) {
        return !contains(seq, searchChar);
    }

    public static boolean equalsIgnoreCase(final CharSequence str1, final CharSequence str2) {
        return !StringUtils.equalsIgnoreCase(str1, str2);
    }

    public static String[] split(String s, String separatorChars) {
        return StringUtils.split(s, separatorChars);
    }

    public static String defaultString(String s) {
        return StringUtils.defaultString(s);
    }

    public static String defaultString(String s, String defaultStr) {
        return StringUtils.defaultString(s, defaultStr);
    }

    public static <T> Collection<T> emptyIfNull(Collection<T> collection) {
        return CollectionUtils.emptyIfNull(collection);
    }

    public static boolean isEmpty(Collection collection) {
        return CollectionUtils.isEmpty(collection);
    }

    public static boolean nonEmpty(Collection collection) {
        return !isEmpty(collection);
    }

    public static <K, V> Map<K, V> emptyIfNull(Map<K, V> map) {
        return MapUtils.emptyIfNull(map);
    }

    public static boolean isEmpty(Map map) {
        return MapUtils.isEmpty(map);
    }

    public static boolean nonEmpty(Map map) {
        return !isEmpty(map);
    }

    public static <T> boolean isEmpty(T[] array) {
        return ArrayUtils.isEmpty(array);
    }

    public static <T> boolean nonEmpty(T[] array) {
        return !isEmpty(array);
    }

    public static String substring(final String str, int start) {
        return StringUtils.substring(str, start);
    }

    public static String substring(final String str, int start, int end) {
        return StringUtils.substring(str, start, end);
    }

    public static boolean isGet(Method method) {
        final int parameterCount = method.getParameterCount();
        if (parameterCount != 0) {
            return false;
        }
        final String methodName = method.getName();
        if (methodName.startsWith("get")) {
            return true;
        }
        Class<?> returnType = method.getReturnType();
        if (isBooleanType(returnType)) {
            return methodName.startsWith("is") || methodName.startsWith("can");
        }
        return false;
    }

    public static boolean isSet(Method method) {
        final String methodName = method.getName();
        return methodName.startsWith("set");
    }

    public static boolean isBooleanType(Class<?> type) {
        return type == Boolean.class || type == Boolean.TYPE;
    }

    public static boolean isIntegerType(Class<?> type) {
        return type == Integer.class || type == Integer.TYPE;
    }

    public static boolean isLongType(Class<?> type) {
        return type == Long.class || type == Long.TYPE;
    }

    public static boolean isFloatType(Class<?> type) {
        return type == Float.class || type == Float.TYPE;
    }

    public static boolean isDoubleType(Class<?> type) {
        return type == Double.class || type == Double.TYPE;
    }
}
