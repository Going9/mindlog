package com.mindlog.global.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Order(50)
public class WarmupConfigLoggingRunner implements ApplicationRunner {

    private static final List<String> TARGET_PREFIXES = List.of(
            "server.",
            "spring.",
            "logging.",
            "mindlog."
    );
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password",
            "secret",
            "token",
            "apikey",
            "anon-key",
            "private",
            "credential"
    );

    private final ConfigurableEnvironment environment;

    public WarmupConfigLoggingRunner(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        var keys = collectCandidateKeys();
        var resolvedValues = new LinkedHashMap<String, String>();

        for (var key : keys) {
            var value = environment.getProperty(key);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            resolvedValues.put(key, maskIfSensitive(key, value));
        }

        var activeProfiles = String.join(",", environment.getActiveProfiles());
        var dump = buildFlatDump(resolvedValues);
        log.info("[CONFIG] Effective config dump - activeProfiles={}\n{}", activeProfiles, dump);
    }

    private List<String> collectCandidateKeys() {
        var keys = new ArrayList<String>();
        for (var propertySource : environment.getPropertySources()) {
            if (!(propertySource instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (var name : enumerable.getPropertyNames()) {
                if (isTargetKey(name)) {
                    keys.add(name);
                }
            }
        }

        return keys.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private boolean isTargetKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        for (var prefix : TARGET_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String maskIfSensitive(String key, String value) {
        var lowerKey = key.toLowerCase();
        for (var keyword : SENSITIVE_KEYWORDS) {
            if (lowerKey.contains(keyword)) {
                return maskValue(value);
            }
        }
        return value;
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "<masked>";
        }
        if (value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private String buildFlatDump(Map<String, String> values) {
        var sb = new StringBuilder();
        for (var entry : values.entrySet()) {
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append('\n');
        }
        return sb.toString();
    }
}
