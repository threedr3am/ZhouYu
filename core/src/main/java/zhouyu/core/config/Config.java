package zhouyu.core.config;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

    private static Config config;

    private static Boolean printError = false;

    public static final Config getInstance() {
        if (config == null) {
            synchronized (Config.class) {
                if (config == null) {
                    config = new Config();
                }
            }
        }
        return config;
    }

    public static void init(String action) throws IllegalAccessException {
        if (action == null || action.isEmpty()) {
            return;
        }
        Config config = getInstance();
        Map<String, Field> fieldMap = new HashMap<>();
        Field[] fields = Config.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("config")) {
                continue;
            }
            fieldMap.put(field.getName(), field);
        }

        Pattern pattern = Pattern.compile("((.+?)=(.+?))(,|$)");
        Matcher matcher = pattern.matcher(action);
        while (matcher.find()) {
            String key = matcher.group(2);
            String value = matcher.group(3);
            Field field;
            if ((field = fieldMap.get(key)) != null) {
                if (field.getType() == Boolean.class) {
                    field.set(config, Boolean.valueOf(value));
                } else if (field.getType() == Integer.class) {
                    field.set(config, Integer.valueOf(value));
                } else if (field.getType() == Long.class) {
                    field.set(config, Long.valueOf(value));
                } else {
                    field.set(config, value);
                }
            }
        }
    }

    public static Boolean getPrintError() {
        return printError;
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
