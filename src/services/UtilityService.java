package services;

/**
 * 通用工具类 - 提供常用的工具方法
 * 遵循单一职责原则，将所有通用的工具方法集中管理
 */
public class UtilityService {

    /**
     * 清空终端屏幕
     * 支持Windows和Unix系统
     */
    public static void clearScreen() {
        try {
            // 检测操作系统
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows系统使用cls命令
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Unix/Linux/Mac系统使用clear命令
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // 如果清屏失败，打印空行
            printEmptyLines(50);
        }
    }

    /**
     * 打印指定数量的空行
     */
    public static void printEmptyLines(int count) {
        for (int i = 0; i < count; i++) {
            System.out.println();
        }
    }

    /**
     * 转义JSON字符串中的特殊字符
     * @param str 要转义的字符串
     * @return 转义后的字符串
     */
    public static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 从JSON字符串中提取指定键的值
     * @param json JSON字符串
     * @param key 要提取的键
     * @return 提取的值，如果未找到则返回null
     */
    public static String extractJsonValue(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        try {
            // 查找键的位置
            String keyPattern = "\"" + key + "\":";
            int keyIndex = json.indexOf(keyPattern);

            if (keyIndex == -1) {
                return null;
            }

            // 移动到值的开始位置
            int valueStart = json.indexOf(':', keyIndex) + 1;

            // 跳过空白字符
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            // 处理不同类型的值
            char firstChar = json.charAt(valueStart);

            if (firstChar == '"') {
                // 字符串值
                return extractStringValue(json, valueStart);
            } else if (firstChar == '{' || firstChar == '[') {
                // 对象或数组值
                return extractComplexValue(json, valueStart, firstChar);
            } else {
                // 数字、布尔值或null
                return extractPrimitiveValue(json, valueStart);
            }

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取字符串值
     */
    private static String extractStringValue(String json, int startIndex) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int i = startIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                // 找到结束引号
                return value.toString();
            } else {
                value.append(c);
            }
        }

        return value.toString();
    }

    /**
     * 提取复杂值（对象或数组）
     */
    private static String extractComplexValue(String json, int startIndex, char startChar) {
        char endChar = startChar == '{' ? '}' : ']';
        int braceCount = 0;
        StringBuilder value = new StringBuilder();

        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            value.append(c);

            if (c == startChar) {
                braceCount++;
            } else if (c == endChar) {
                braceCount--;
                if (braceCount == 0) {
                    return value.toString();
                }
            }
        }

        return value.toString();
    }

    /**
     * 提取原始值（数字、布尔值、null）
     */
    private static String extractPrimitiveValue(String json, int startIndex) {
        StringBuilder value = new StringBuilder();

        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == ',' || c == '}' || c == ']') {
                break;
            }

            value.append(c);
        }

        return value.toString().trim();
    }

    /**
     * 从JSON中提取完整的对象
     * @param json JSON字符串
     * @param key 对象键名
     * @return 提取的对象JSON字符串
     */
    public static String extractJsonObject(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        try {
            String value = extractJsonValue(json, key);
            if (value != null && (value.startsWith("{") || value.startsWith("["))) {
                return value;
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }

        return null;
    }

    /**
     * 分割JSON对象数组
     * @param json JSON数组字符串
     * @return 分割后的JSON对象数组
     */
    public static String[] splitJsonObjects(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new String[0];
        }

        java.util.List<String> objects = new java.util.ArrayList<>();
        int depth = 0;
        int startIndex = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (c == '{') {
                if (depth == 0) {
                    startIndex = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && startIndex != -1) {
                    String object = json.substring(startIndex, i + 1);
                    objects.add(object.trim());
                    startIndex = -1;
                }
            }
        }

        return objects.toArray(new String[0]);
    }

    /**
     * 验证邮箱格式
     * @param email 邮箱地址
     * @return 是否为有效邮箱
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                           "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        return email.matches(emailRegex);
    }

    /**
     * 验证电话号码格式
     * @param phoneNumber 电话号码
     * @return 是否为有效电话号码
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // 简单验证：只包含数字、空格、破折号、加号
        String phoneRegex = "^[\\d\\s\\-\\+]+$";
        return phoneRegex.matches(phoneNumber) && phoneNumber.length() >= 8;
    }

    /**
     * 获取当前日期时间字符串
     * @return 格式化的日期时间字符串
     */
    public static String getCurrentDateTime() {
        return java.time.LocalDateTime.now().toString();
    }

    /**
     * 暂停程序执行指定毫秒数
     * @param milliseconds 暂停时间（毫秒）
     */
    public static void pause(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
