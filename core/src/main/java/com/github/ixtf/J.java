package com.github.ixtf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.ixtf.cli.SaferExec;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.ixtf.Constant.MAPPER;
import static com.github.ixtf.Constant.YAML_MAPPER;
import static java.util.Optional.ofNullable;

public class J {

    @SneakyThrows
    public static <T> Class<T> actualClass(Class<T> clazz) {
        final var className = clazz.getName();
        if (J.contains(className, "CGLIB")) {
            final String actualClassName = J.split(className, "$$")[0];
            return (Class<T>) Class.forName(actualClassName);
        }
        return clazz;
    }

    @SneakyThrows({SocketException.class, UnknownHostException.class})
    public static String localIp() {
        @Cleanup final var socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        return socket.getLocalAddress().getHostAddress();
    }

    public static <V> String strTpl(final String tpl, Map<String, V> map) {
        return StringSubstitutor.replace(tpl, map);
    }

    public static String exeCli(final String command) {
        final var exec = new SaferExec();
        return exec.exec(command);
    }

    public static LocalDateTime localDateTime(Date date) {
        return ofNullable(date)
                .map(Date::toInstant)
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElseThrow();
    }

    public static LocalDate localDate(Date date) {
        return ofNullable(date)
                .map(J::localDateTime)
                .map(LocalDateTime::toLocalDate)
                .orElseThrow();
    }

    public static LocalTime localTime(Date date) {
        return ofNullable(date)
                .map(J::localDateTime)
                .map(LocalDateTime::toLocalTime)
                .orElseThrow();
    }

    public static Date date(LocalDateTime ldt) {
        return ofNullable(ldt)
                .map(it -> it.atZone(ZoneId.systemDefault()))
                .map(ZonedDateTime::toInstant)
                .map(Date::from)
                .orElseThrow();
    }

    public static Date date(LocalDate ld, LocalTime lt) {
        return ofNullable(ld)
                .flatMap(__ -> ofNullable(lt).map(___ -> ld.atTime(lt)))
                .map(J::date)
                .orElseThrow();
    }

    public static Date date(LocalDate ld) {
        return ofNullable(ld)
                .map(LocalDate::atStartOfDay)
                .map(J::date)
                .orElseThrow();
    }

    public static Date date(LocalTime lt) {
        return ofNullable(lt)
                .map(it -> it.atDate(LocalDate.now()))
                .map(J::date)
                .orElseThrow();
    }

    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    public static <E> ArrayList<E> newArrayList(E... elements) {
        final var capacity = 5 + elements.length + elements.length / 10;
        final var list = new ArrayList<E>(capacity);
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

    @SneakyThrows(IOException.class)
    public static void moveFile(File srcFile, File destFile) {
        FileUtils.moveFile(srcFile, destFile);
    }

    /**
     * @param file 文件扩展名 json 或 yml 或 yaml 可以使用
     * @return {@link JsonNode}
     */
    @SneakyThrows(IOException.class)
    public static JsonNode readJson(File file) {
        final var ext = FilenameUtils.getExtension(file.getName());
        if ("yml".equalsIgnoreCase(ext) || "yaml".equalsIgnoreCase(ext)) {
            return YAML_MAPPER.readTree(file);
        }
        return MAPPER.readTree(file);
    }

    @SneakyThrows(IOException.class)
    public static void writeJson(File file, Object o) {
        FileUtils.forceMkdirParent(file);
        final var ext = FilenameUtils.getExtension(file.getName());
        if ("yml".equalsIgnoreCase(ext) || "yaml".equalsIgnoreCase(ext)) {
            YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, o);
        } else {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, o);
        }
    }

    public static void writeJson(String file, Object o) {
        writeJson(J.getFile(file), o);
    }

    @SneakyThrows(IOException.class)
    public static JsonNode readJson(String json) {
        return MAPPER.readTree(json);
    }

    public static JsonNode readJsonFile(String s) {
        return readJson(getFile(s));
    }

    @SneakyThrows(IOException.class)
    public static JsonNode readJson(byte[] bytes) {
        return MAPPER.readTree(bytes);
    }

    @SneakyThrows(IOException.class)
    public static String toJson(Object o) {
        return MAPPER.writeValueAsString(o);
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

    public static <T> T checkAndGetCommand(T command) {
        final var validatorFactory = Validation.buildDefaultValidatorFactory();
        final var validator = validatorFactory.getValidator();
        final var violations = validator.validate(command);
        if (J.nonEmpty(violations)) {
            throw new ConstraintViolationException(violations);
        }
        return command;
    }

    @SneakyThrows(IOException.class)
    public static <T> T checkAndGetCommand(Class<T> clazz, byte[] bytes) {
        final T command = MAPPER.readValue(bytes, clazz);
        return checkAndGetCommand(command);
    }

    @SneakyThrows(JsonProcessingException.class)
    public static <T> T checkAndGetCommand(Class<T> clazz, String json) {
        final T command = MAPPER.readValue(json, clazz);
        return checkAndGetCommand(command);
    }

    public static <T> T checkAndGetCommand(Class<T> clazz, JsonNode jsonNode) {
        final T command = MAPPER.convertValue(jsonNode, clazz);
        return checkAndGetCommand(command);
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
