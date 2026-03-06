package com.ratelimiter.config;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.Errors.ConfigValidationException;
import com.ratelimiter.model.Errors.PolicyNotFoundException;
import com.ratelimiter.model.RateLimitPolicy;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class PolicyLoader {
    private static final String DEFAULT_RESOURCE_PATH = "config/policies.yaml";

    private volatile Map<String, RateLimitPolicy> policyMap = Map.of();

    public PolicyLoader() {
        this(loadDefaultPolicies());
    }

    PolicyLoader(List<RateLimitPolicy> initialPolicies) {
        reload(initialPolicies);
    }

    public RateLimitPolicy getPolicy(String id) {
        RateLimitPolicy policy = policyMap.get(id);
        if (policy == null) {
            throw new PolicyNotFoundException("Policy not found: " + id);
        }
        return policy;
    }

    public void reload(List<RateLimitPolicy> newPolicies) {
        List<String> errors = new ArrayList<>();
        Map<String, RateLimitPolicy> validated = validateAndBuildMap(newPolicies, errors);
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }

        synchronized (this) {
            policyMap = Map.copyOf(validated);
        }
    }

    private static List<RateLimitPolicy> loadDefaultPolicies() {
        InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(DEFAULT_RESOURCE_PATH);

        if (stream == null) {
            throw new ConfigValidationException(List.of("Missing resource: " + DEFAULT_RESOURCE_PATH));
        }

        return parsePolicies(stream);
    }

    static List<RateLimitPolicy> parsePolicies(InputStream yamlStream) {
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(yamlStream);
        if (!(loaded instanceof Map<?, ?> rootMap)) {
            throw new ConfigValidationException(List.of("YAML root must be an object with a 'policies' list"));
        }

        Object policiesObj = rootMap.get("policies");
        if (!(policiesObj instanceof List<?> policyEntries)) {
            throw new ConfigValidationException(List.of("'policies' must be a list"));
        }

        List<Map<String, Object>> policyMaps = new ArrayList<>();
        for (Object entry : policyEntries) {
            if (!(entry instanceof Map<?, ?> entryMap)) {
                throw new ConfigValidationException(List.of("Each policy entry must be an object"));
            }

            Map<String, Object> normalized = new HashMap<>();
            entryMap.forEach((k, v) -> normalized.put(String.valueOf(k), v));
            policyMaps.add(normalized);
        }

        return mapAndValidate(policyMaps);
    }

    private static List<RateLimitPolicy> mapAndValidate(List<Map<String, Object>> rawPolicies) {
        List<RateLimitPolicy> policies = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rawPolicies.size(); i++) {
            Map<String, Object> row = rawPolicies.get(i);
            String id = asString(row.get("id"));
            String policyRef = policyRef(id, i);

            Integer limit = asInteger(row.get("limit"));
            Algorithm algorithm = parseAlgorithm(row.get("algorithm"), policyRef, errors);
            Long windowMs = asLong(row.get("windowMs"));
            Integer refillRate = asInteger(row.get("refillRate"));
            Integer capacity = asInteger(row.get("capacity"));
            Integer leakRate = asInteger(row.get("leakRate"));

            if (isBlank(id)) {
                errors.add(policyRef + ": id must be non-empty");
            }
            if (limit == null) {
                errors.add(policyRef + ": limit is required");
            }

            if (algorithm != null) {
                if (algorithm == Algorithm.SLIDING_WINDOW && (windowMs == null || windowMs <= 0)) {
                    errors.add(policyRef + ": windowMs must be > 0 for SLIDING_WINDOW");
                }
                if (algorithm == Algorithm.TOKEN_BUCKET) {
                    if (refillRate == null || refillRate <= 0) {
                        errors.add(policyRef + ": refillRate must be > 0 for TOKEN_BUCKET");
                    }
                    if (capacity == null || capacity <= 0) {
                        errors.add(policyRef + ": capacity must be > 0 for TOKEN_BUCKET");
                    }
                }
                if (algorithm == Algorithm.LEAKY_BUCKET) {
                    if (leakRate == null || leakRate <= 0) {
                        errors.add(policyRef + ": leakRate must be > 0 for LEAKY_BUCKET");
                    }
                    if (capacity == null || capacity <= 0) {
                        errors.add(policyRef + ": capacity must be > 0 for LEAKY_BUCKET");
                    }
                }
            }

            if (limit != null && limit <= 0) {
                errors.add(policyRef + ": limit must be > 0");
            }

            policies.add(new RateLimitPolicy(
                    defaultIfBlank(id, "unknown-" + i),
                    algorithm == null ? Algorithm.TOKEN_BUCKET : algorithm,
                    limit == null ? 0 : limit,
                    windowMs == null ? 0L : windowMs,
                    refillRate == null ? 0 : refillRate,
                    capacity == null ? 0 : capacity,
                    leakRate == null ? 0 : leakRate
            ));
        }

        Map<String, RateLimitPolicy> byId = validateAndBuildMap(policies, errors);
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }

        return new ArrayList<>(byId.values());
    }

    private static Algorithm parseAlgorithm(Object raw, String policyRef, List<String> errors) {
        String algorithmRaw = asString(raw);
        if (isBlank(algorithmRaw)) {
            errors.add(policyRef + ": algorithm is required");
            return null;
        }

        try {
            return Algorithm.valueOf(algorithmRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            errors.add(policyRef + ": unsupported algorithm: " + algorithmRaw);
            return null;
        }
    }

    private static Map<String, RateLimitPolicy> validateAndBuildMap(List<RateLimitPolicy> policies, List<String> errors) {
        Map<String, RateLimitPolicy> byId = new HashMap<>();

        for (RateLimitPolicy policy : policies) {
            String ref = "policy[" + policy.id() + "]";
            if (isBlank(policy.id())) {
                errors.add(ref + ": id must be non-empty");
            }

            if (policy.limit() <= 0) {
                errors.add(ref + ": limit must be > 0");
            }

            if (policy.algorithm() == Algorithm.SLIDING_WINDOW && policy.windowMs() <= 0) {
                errors.add(ref + ": windowMs must be > 0 for SLIDING_WINDOW");
            }
            if (policy.algorithm() == Algorithm.TOKEN_BUCKET && (policy.refillRate() <= 0 || policy.capacity() <= 0)) {
                errors.add(ref + ": refillRate and capacity must be > 0 for TOKEN_BUCKET");
            }
            if (policy.algorithm() == Algorithm.LEAKY_BUCKET && (policy.leakRate() <= 0 || policy.capacity() <= 0)) {
                errors.add(ref + ": leakRate and capacity must be > 0 for LEAKY_BUCKET");
            }

            RateLimitPolicy previous = byId.putIfAbsent(policy.id(), policy);
            if (previous != null) {
                errors.add(ref + ": duplicate policy id");
            }
        }

        return byId;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static String policyRef(String id, int index) {
        return isBlank(id) ? "policy[index=" + index + "]" : "policy[" + id + "]";
    }
}
