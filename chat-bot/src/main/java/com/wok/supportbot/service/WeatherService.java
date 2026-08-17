package com.wok.supportbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 天气查询服务
 * 支持日期查询：今天、明天、后天、指定日期（如2026-08-20、8月20日）等
 * 超出预报范围的日期会给出友好提示
 */
@Service
@Slf4j
public class WeatherService {

    // 支持的天气预报范围（天）
    private static final int MAX_FORECAST_RANGE = 7;
    private static final int MAX_HISTORY_RANGE = 7;

    private static final Map<String, String[]> CITY_BASE_WEATHER = new HashMap<>();
    private static final String[] WEATHER_TYPES = {"晴", "多云", "阴", "阵雨", "雷阵雨", "小雨", "晴", "多云"};

    // 日期正则表达式模式
    private static final Pattern PATTERN_YYYY_MM_DD = Pattern.compile("(\\d{4})[年\\-/.](\\d{1,2})[月\\-/.](\\d{1,2})");
    private static final Pattern PATTERN_MM_DD = Pattern.compile("(\\d{1,2})[月/、.](\\d{1,2})[日号]?");

    static {
        CITY_BASE_WEATHER.put("北京", new String[]{"晴", "28", "18", "微风", "湿度45%", "空气质量优"});
        CITY_BASE_WEATHER.put("上海", new String[]{"多云", "32", "24", "东南风3级", "湿度72%", "空气质量良"});
        CITY_BASE_WEATHER.put("广州", new String[]{"阵雨", "35", "26", "南风4级", "湿度85%", "空气质量良"});
        CITY_BASE_WEATHER.put("深圳", new String[]{"雷阵雨", "34", "27", "南风3级", "湿度80%", "空气质量良"});
        CITY_BASE_WEATHER.put("杭州", new String[]{"多云", "30", "22", "东风2级", "湿度65%", "空气质量优"});
        CITY_BASE_WEATHER.put("成都", new String[]{"阴", "26", "18", "微风", "湿度70%", "空气质量良"});
        CITY_BASE_WEATHER.put("武汉", new String[]{"晴", "31", "20", "微风", "湿度55%", "空气质量优"});
        CITY_BASE_WEATHER.put("西安", new String[]{"晴", "29", "16", "西北风3级", "湿度35%", "空气质量良"});
        CITY_BASE_WEATHER.put("南京", new String[]{"多云", "30", "22", "东风3级", "湿度68%", "空气质量良"});
        CITY_BASE_WEATHER.put("重庆", new String[]{"阴", "28", "20", "微风", "湿度75%", "空气质量良"});
        CITY_BASE_WEATHER.put("天津", new String[]{"晴", "27", "15", "西北风3级", "湿度40%", "空气质量良"});
        CITY_BASE_WEATHER.put("苏州", new String[]{"多云", "31", "23", "东风2级", "湿度70%", "空气质量优"});
    }

    /**
     * 查询指定城市天气（默认今天）
     */
    public Map<String, Object> getWeather(String city) {
        return getWeather(city, 0);
    }

    /**
     * 查询指定城市指定日期的天气
     */
    public Map<String, Object> getWeather(String city, int dateOffset) {
        if (city == null || city.trim().isEmpty()) {
            return getDefaultWeather(dateOffset);
        }

        String matchedCity = matchCity(city);
        if (matchedCity == null) {
            return buildUnknownCityResult(city, dateOffset);
        }

        // 检查日期是否超出范围
        String rangeWarning = checkDateRangeWarning(dateOffset);
        if (rangeWarning != null) {
            return buildOutOfRangeResult(matchedCity, dateOffset, rangeWarning);
        }

        String[] base = CITY_BASE_WEATHER.get(matchedCity);
        return buildWeatherResult(matchedCity, base, dateOffset);
    }

    /**
     * 根据日期描述查询天气（支持自然语言日期）
     * 支持："今天"、"明天"、"后天"、"2026-08-20"、"8月20日"、"下周一"等
     */
    public Map<String, Object> getWeatherByDateDesc(String city, String dateDesc) {
        // 尝试解析为具体日期
        LocalDate parsedDate = tryParseDate(dateDesc);
        if (parsedDate != null) {
            int offset = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), parsedDate);
            log.info("解析日期描述 [{}] 为具体日期 {}, 偏移={}", dateDesc, parsedDate, offset);
            return getWeather(city, offset);
        }

        // 回退到原来的偏移量解析
        int offset = parseDateOffset(dateDesc);
        return getWeather(city, offset);
    }

    /**
     * 尝试将日期描述解析为具体日期
     */
    private LocalDate tryParseDate(String dateDesc) {
        if (dateDesc == null || dateDesc.trim().isEmpty()) {
            return null;
        }

        String desc = dateDesc.trim();
        LocalDate today = LocalDate.now();

        // 格式1: 2026年8月20日 或 2026-08-20 或 2026/8/20
        Matcher m1 = PATTERN_YYYY_MM_DD.matcher(desc);
        if (m1.find()) {
            try {
                int year = Integer.parseInt(m1.group(1));
                int month = Integer.parseInt(m1.group(2));
                int day = Integer.parseInt(m1.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                log.warn("解析完整日期失败: {}", e.getMessage());
            }
        }

        // 格式2: 8月20日 或 8/20
        Matcher m2 = PATTERN_MM_DD.matcher(desc);
        if (m2.find()) {
            try {
                int month = Integer.parseInt(m2.group(1));
                int day = Integer.parseInt(m2.group(2));
                // 默认使用今年，如果已过则使用明年
                int year = today.getYear();
                try {
                    LocalDate candidate = LocalDate.of(year, month, day);
                    if (candidate.isBefore(today.minusYears(1))) {
                        return null; // 太旧的日期，不支持
                    }
                    return candidate;
                } catch (Exception e) {
                    // 日期无效，如2月30日
                    return null;
                }
            } catch (Exception e) {
                log.warn("解析月日失败: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * 检查日期是否超出预报范围
     */
    private String checkDateRangeWarning(int dateOffset) {
        if (dateOffset > MAX_FORECAST_RANGE) {
            return String.format("抱歉哦～目前天气数据只支持查询未来%d天内的天气，超出范围的日期暂时无法查询呢😅", MAX_FORECAST_RANGE);
        }
        if (dateOffset < -MAX_HISTORY_RANGE) {
            return String.format("抱歉哦～目前天气数据只支持查询过去%d天内的天气，更早的日期暂时无法查询呢😅", MAX_HISTORY_RANGE);
        }
        return null;
    }

    /**
     * 构建超出范围的天气结果
     */
    private Map<String, Object> buildOutOfRangeResult(String city, int dateOffset, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate targetDate = LocalDate.now().plusDays(dateOffset);
        result.put("city", city);
        result.put("weather", "未知");
        result.put("high", "--");
        result.put("low", "--");
        result.put("date", targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        result.put("dateLabel", getDateLabel(targetDate, dateOffset));
        result.put("outOfRange", true);
        result.put("message", message);
        return result;
    }

    /**
     * 解析日期描述为日期偏移量（兼容旧逻辑）
     */
    public int parseDateOffset(String dateDesc) {
        if (dateDesc == null || dateDesc.trim().isEmpty()) {
            return 0;
        }
        String desc = dateDesc.trim();

        // 直接的数字
        try {
            return Integer.parseInt(desc);
        } catch (NumberFormatException ignored) {
        }

        // 大后天
        if (desc.contains("大后天")) return 3;
        // 后天
        if (desc.contains("后天")) return 2;
        // 明天/明日
        if (desc.contains("明天") || desc.contains("明日") || desc.contains("明儿")) return 1;
        // 今天/今日
        if (desc.contains("今天") || desc.contains("今日") || desc.contains("现在") || desc.contains("当前")) return 0;
        // 昨天
        if (desc.contains("昨天") || desc.contains("昨日")) return -1;
        // 周末
        if (desc.contains("周末")) return getWeekendOffset();
        // 下周
        if (desc.contains("下周")) return parseNextWeekdayOffset(desc);
        // 本周/这周 + 星期几
        if ((desc.contains("本周") || desc.contains("这周") || desc.contains("这个星期")) && (desc.contains("周") || desc.contains("星期"))) {
            return parseWeekdayOffsetFromContext(desc, false);
        }
        // 下周末
        if (desc.contains("下周末")) return getNextWeekendOffset();
        // 周/星期几
        if (desc.contains("周") || desc.contains("星期")) return parseWeekdayOffset(desc);

        // 默认今天
        return 0;
    }

    /**
     * 解析星期偏移
     */
    private int parseWeekdayOffset(String desc) {
        LocalDate today = LocalDate.now();
        int currentDayOfWeek = today.getDayOfWeek().getValue();

        int targetDay = -1;
        if (desc.contains("一") || desc.contains("1")) targetDay = 1;
        else if (desc.contains("二") || desc.contains("2")) targetDay = 2;
        else if (desc.contains("三") || desc.contains("3")) targetDay = 3;
        else if (desc.contains("四") || desc.contains("4")) targetDay = 4;
        else if (desc.contains("五") || desc.contains("5")) targetDay = 5;
        else if (desc.contains("六") || desc.contains("6")) targetDay = 6;
        else if (desc.contains("日") || desc.contains("天") || desc.contains("7")) targetDay = 7;

        if (targetDay < 1) return 0;

        int diff = targetDay - currentDayOfWeek;
        if (diff <= 0) diff += 7;
        return diff;
    }

    /**
     * 解析下周的星期偏移
     */
    private int parseNextWeekdayOffset(String desc) {
        int baseOffset = parseWeekdayOffset(desc.replace("下周", "").replace("下", ""));
        if (baseOffset == 0) return 7;
        return baseOffset + 7;
    }

    /**
     * 获取周末偏移
     */
    private int getWeekendOffset() {
        LocalDate today = LocalDate.now();
        int currentDayOfWeek = today.getDayOfWeek().getValue();
        if (currentDayOfWeek >= 6) {
            return 6 + (7 - currentDayOfWeek);
        }
        return 6 - currentDayOfWeek;
    }

    /**
     * 获取下周末偏移
     */
    private int getNextWeekendOffset() {
        return getWeekendOffset() + 7;
    }

    /**
     * 从上下文解析星期几
     */
    private int parseWeekdayOffsetFromContext(String desc, boolean isNextWeek) {
        return parseWeekdayOffset(desc);
    }

    /**
     * 匹配城市名
     */
    private String matchCity(String city) {
        if (CITY_BASE_WEATHER.containsKey(city)) {
            return city;
        }
        for (String key : CITY_BASE_WEATHER.keySet()) {
            if (city.contains(key) || key.contains(city)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 构建天气结果
     */
    private Map<String, Object> buildWeatherResult(String city, String[] base, int dateOffset) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("city", city);

        int weatherIndex = Math.abs((city.hashCode() + dateOffset * 7) % WEATHER_TYPES.length);
        String weather = WEATHER_TYPES[weatherIndex];

        int baseHigh = Integer.parseInt(base[1]);
        int baseLow = Integer.parseInt(base[2]);
        int tempVariance = (dateOffset * 3 + city.hashCode() % 5) % 5 - 2;
        int high = Math.max(0, Math.min(45, baseHigh + tempVariance));
        int low = Math.max(-5, Math.min(35, baseLow + tempVariance - 1));

        data.put("weather", weather);
        data.put("high", high);
        data.put("low", low);
        data.put("wind", base[3]);
        data.put("humidity", base[4]);
        data.put("airQuality", base[5]);

        LocalDate targetDate = LocalDate.now().plusDays(dateOffset);
        String dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dateLabel = getDateLabel(targetDate, dateOffset);
        data.put("date", dateStr);
        data.put("dateLabel", dateLabel);
        data.put("updatedAt", new Date().toString());
        data.put("tip", buildWeatherTip(weather, high));

        return data;
    }

    /**
     * 获取日期标签
     */
    private String getDateLabel(LocalDate date, int offset) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            String[] weekdays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            String weekday = weekdays[dayOfWeek];
            return date.format(DateTimeFormatter.ofPattern("M月d日")) + "（" + weekday + "）";
        }

        switch (offset) {
            case 0: return "今天";
            case 1: return "明天";
            case 2: return "后天";
            case 3: return "大后天";
            case -1: return "昨天";
            default:
                String[] weekdays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                String weekday = weekdays[dayOfWeek];
                return date.format(DateTimeFormatter.ofPattern("M月d日")) + "（" + weekday + "）";
        }
    }

    /**
     * 构建天气温馨提示
     */
    private String buildWeatherTip(String weather, int high) {
        if (high >= 35) {
            return "天气炎热，注意防暑降温，多喝水哦~";
        } else if (high <= 10) {
            return "天气寒冷，注意保暖添衣哦~";
        } else if ("雷阵雨".equals(weather)) {
            return "有雷阵雨，出门记得带伞，注意安全哦~";
        } else if ("阵雨".equals(weather) || "小雨".equals(weather)) {
            return "今天有雨，出门记得带伞哦~";
        } else if ("晴".equals(weather)) {
            return "阳光明媚，适合出门活动~";
        } else if ("多云".equals(weather)) {
            return "天气不错，祝您有个好心情~";
        } else {
            return "天气不错，出门注意休息~";
        }
    }

    /**
     * 构建未知城市结果
     */
    private Map<String, Object> buildUnknownCityResult(String city, int dateOffset) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate targetDate = LocalDate.now().plusDays(dateOffset);
        result.put("city", city);
        result.put("weather", "未知");
        result.put("high", "--");
        result.put("low", "--");
        result.put("date", targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        result.put("dateLabel", getDateLabel(targetDate, dateOffset));
        result.put("message", "抱歉，暂未收录该城市的天气信息");
        return result;
    }

    /**
     * 获取默认天气（北京）
     */
    public Map<String, Object> getDefaultWeather(int dateOffset) {
        return buildWeatherResult("北京", CITY_BASE_WEATHER.get("北京"), dateOffset);
    }

    /**
     * 获取所有支持的城市列表
     */
    public List<String> getSupportedCities() {
        return new ArrayList<>(CITY_BASE_WEATHER.keySet());
    }

    /**
     * 格式化天气信息为可读文本
     */
    public String formatWeather(String city, Map<String, Object> weather) {
        // 如果是超出范围的结果，返回提示信息
        Object outOfRange = weather.get("outOfRange");
        if (outOfRange != null && Boolean.TRUE.equals(outOfRange)) {
            Object msg = weather.get("message");
            if (msg != null) {
                return msg.toString();
            }
        }

        // 如果城市未找到
        Object weatherDesc = weather.get("weather");
        if ("未知".equals(weatherDesc)) {
            Object msg = weather.get("message");
            if (msg != null) {
                return msg.toString();
            }
            return "抱歉哦～暂时没查到" + city + "的天气信息呢😅";
        }

        StringBuilder sb = new StringBuilder();
        String cityName = weather.get("city").toString();
        String dateLabel = weather.get("dateLabel") != null ? weather.get("dateLabel").toString() : "今天";

        sb.append("☀️ 【天气预报】").append(cityName);
        if (!dateLabel.isEmpty()) {
            sb.append("（").append(dateLabel).append("）");
        }
        sb.append("\n\n");
        sb.append("🌤 天气：").append(weatherDesc).append("\n");
        sb.append("🌡 温度：").append(weather.get("low")).append("℃ ~ ").append(weather.get("high")).append("℃\n");
        sb.append("💨 风力：").append(weather.get("wind")).append("\n");
        sb.append("💧 湿度：").append(weather.get("humidity")).append("\n");
        sb.append("🌿 空气质量：").append(weather.get("airQuality")).append("\n\n");

        Object tip = weather.get("tip");
        if (tip != null) {
            sb.append("💡 ").append(tip);
        }

        return sb.toString();
    }
}
