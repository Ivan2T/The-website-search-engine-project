package searchengine.utills.parsing.sitemapping;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.regex.Pattern;

public class Utils {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";

    public static String getProtocolAndDomain(String url) {
        if (url == null || url.isEmpty()) {
            // Логируем предупреждение о пустом URL
            System.err.println(ANSI_RED + "Получен пустой или null URL" + ANSI_RESET);
            return "default_value"; // Возвращаем значение по умолчанию
        }

        // Обновленное регулярное выражение для обработки http и https
        String regEx = "^(https?:\\/\\/)(?:[^@\\/\\n]+@)?(?:www\\.)?([^:\\/\\n]+)";
        Pattern pattern = Pattern.compile(regEx);
        Optional<String> result = pattern.matcher(url)
                .results()
                .map(m -> m.group(1) + m.group(2))
                .findFirst();

        // Проверяем наличие результата и возвращаем его или значение по умолчанию
        return result.orElseGet(() -> {
            System.err.println(ANSI_RED + "Не удалось извлечь протокол и домен из URL: " + url + ANSI_RESET);
            return "default_value"; // Замените на подходящее значение по умолчанию
        });
    }

    public static Timestamp setNow() {
        return new Timestamp(System.currentTimeMillis());
    }
}
