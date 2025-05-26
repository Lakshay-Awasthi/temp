// src/main/java/com/example/ruleengine/service/RuleEngineService.java
package com.example.ruleengine.service;

import com.example.ruleengine.evaluator.RuleEvaluator;
import com.example.ruleengine.loader.RuleLoader;
import com.example.ruleengine.model.Rule;
import com.example.ruleengine.model.RuleEvaluationResult;
import com.example.ruleengine.model.RuleSet;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced service for hierarchical rule engine operations with SpEL support
 */
@Service
public class RuleEngineService {
    
    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);
    
    private final List<RuleLoader> ruleLoaders;
    private final RuleEvaluator ruleEvaluator; // Single evaluator for hierarchical rules
    private final Map<String, List<Rule>> loadedRules = new ConcurrentHashMap<>();
    
    public RuleEngineService(List<RuleLoader> ruleLoaders, RuleEvaluator ruleEvaluator) {
        this.ruleLoaders = ruleLoaders;
        this.ruleEvaluator = ruleEvaluator;
    }
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing Rule Engine Service with {} loaders and hierarchical SpEL evaluator", 
            ruleLoaders.size());
    }
    
    /**
     * Load rules from a source file
     */
    public void loadRules(String source) throws IOException {
        log.info("Loading rules from source: {}", source);
        
        RuleLoader loader = ruleLoaders.stream()
            .filter(l -> l.supports(source))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No loader found for source: " + source));
        
        RuleSet ruleSet = loader.loadRules(source);
        loadedRules.put(source, ruleSet.rules());
        
        log.info("Successfully loaded {} rules from {}", ruleSet.rules().size(), source);
    }
    
    /**
     * Load rules from multiple sources
     */
    public void loadRules(List<String> sources) {
        sources.forEach(source -> {
            try {
                loadRules(source);
            } catch (IOException e) {
                log.error("Failed to load rules from {}: {}", source, e.getMessage());
            }
        });
    }
    
    /**
     * Evaluate a single rule against data (supports both Map and POJO)
     */
    public RuleEvaluationResult evaluateRule(Rule rule, Object data) {
        if (!rule.isEnabled()) {
            return RuleEvaluationResult.failure(rule.name(), "Rule is disabled", 0);
        }
        
        return ruleEvaluator.evaluate(rule, data);
    }
    
    /**
     * Evaluate a single rule against data map (backward compatibility)
     */
    public RuleEvaluationResult evaluateRule(Rule rule, Map<String, Object> data) {
        return evaluateRule(rule, (Object) data);
    }
    
    /**
     * Evaluate all loaded rules against data
     */
    public List<RuleEvaluationResult> evaluateAllRules(Object data) {
        return getAllRules().stream()
            .filter(Rule::isEnabled)
            .sorted(Comparator.comparingInt(Rule::priority))
            .map(rule -> evaluateRule(rule, data))
            .toList();
    }
    
    /**
     * Evaluate all loaded rules against data map (backward compatibility)
     */
    public List<RuleEvaluationResult> evaluateAllRules(Map<String, Object> data) {
        return evaluateAllRules((Object) data);
    }
    
    /**
     * Evaluate rules from a specific source
     */
    public List<RuleEvaluationResult> evaluateRulesFromSource(String source, Object data) {
        List<Rule> rules = loadedRules.get(source);
        if (rules == null || rules.isEmpty()) {
            log.warn("No rules found for source: {}", source);
            return List.of();
        }// pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>rule-engine</artifactId>
    <version>1.0.0</version>
    <name>Dynamic Rule Engine</name>
    
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-xml</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

// src/main/java/com/example/ruleengine/model/Rule.java
package com.example.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enhanced rule model supporting hierarchical conditions with SpEL
 */
public record Rule(
    String id,
    String name,
    @JsonProperty("priority") int priority,
    @JsonProperty("enabled") boolean isEnabled,
    ConditionGroup rootConditionGroup
) {
    
    public Rule {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Rule id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be null or blank");
        }
        if (rootConditionGroup == null) {
            throw new IllegalArgumentException("Root condition group cannot be null");
        }
    }
    
    // Default values for backward compatibility
    public Rule(String id, String name, ConditionGroup rootConditionGroup) {
        this(id, name, 0, true, rootConditionGroup);
    }
}

// src/main/java/com/example/ruleengine/model/ConditionGroup.java
package com.example.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a group of conditions with logical operators
 */
public record ConditionGroup(
    LogicalOperator logicalOperator,
    List<ConditionComponent> components
) {
    
    public ConditionGroup {
        if (logicalOperator == null) {
            throw new IllegalArgumentException("Logical operator cannot be null");
        }
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Components cannot be null or empty");
        }
    }
    
    public enum LogicalOperator {
        AND, OR
    }
}

// src/main/java/com/example/ruleengine/model/ConditionComponent.java
package com.example.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents either a single condition or a nested condition group
 */
public record ConditionComponent(
    @JsonProperty("condition") Condition condition,
    @JsonProperty("conditionGroup") ConditionGroup conditionGroup
) {
    
    public ConditionComponent {
        if (condition == null && conditionGroup == null) {
            throw new IllegalArgumentException("Either condition or conditionGroup must be provided");
        }
        if (condition != null && conditionGroup != null) {
            throw new IllegalArgumentException("Cannot have both condition and conditionGroup");
        }
    }
    
    /**
     * Check if this component is a single condition
     */
    public boolean isCondition() {
        return condition != null;
    }
    
    /**
     * Check if this component is a condition group
     */
    public boolean isConditionGroup() {
        return conditionGroup != null;
    }
}

// src/main/java/com/example/ruleengine/model/Condition.java
package com.example.ruleengine.model;

/**
 * Represents a single condition with SpEL expression
 */
public record Condition(
    String expression
) {
    
    public Condition {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression cannot be null or blank");
        }
    }
}

// src/main/java/com/example/ruleengine/model/RuleEvaluationResult.java
package com.example.ruleengine.model;

import java.time.LocalDateTime;

/**
 * Result of rule evaluation
 */
public record RuleEvaluationResult(
    String ruleName,
    boolean matched,
    String reason,
    LocalDateTime evaluatedAt,
    long executionTimeMs
) {
    
    public static RuleEvaluationResult success(String ruleName, long executionTimeMs) {
        return new RuleEvaluationResult(ruleName, true, "Rule matched", LocalDateTime.now(), executionTimeMs);
    }
    
    public static RuleEvaluationResult failure(String ruleName, String reason, long executionTimeMs) {
        return new RuleEvaluationResult(ruleName, false, reason, LocalDateTime.now(), executionTimeMs);
    }
}

// src/main/java/com/example/ruleengine/model/RuleSet.java
package com.example.ruleengine.model;

import java.util.List;

/**
 * Container for multiple rules
 */
public record RuleSet(
    String name,
    String version,
    List<Rule> rules
) {
    
    public RuleSet {
        if (rules == null) {
            throw new IllegalArgumentException("Rules list cannot be null");
        }
    }
    
    // Constructor for rules-only format
    public RuleSet(List<Rule> rules) {
        this("Default", "1.0", rules);
    }
}

// src/main/java/com/example/ruleengine/config/RuleEngineProperties.java
package com.example.ruleengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the rule engine
 */
@ConfigurationProperties(prefix = "rule-engine")
public record RuleEngineProperties(
    List<String> ruleFiles,
    String defaultRulesPath,
    boolean enableCaching,
    int cacheExpirationMinutes,
    boolean enableMetrics
) {
    
    public RuleEngineProperties {
        if (ruleFiles == null) {
            ruleFiles = List.of();
        }
        if (defaultRulesPath == null) {
            defaultRulesPath = "classpath:rules/";
        }
    }
}

// src/main/java/com/example/ruleengine/loader/RuleLoader.java
package com.example.ruleengine.loader;

import com.example.ruleengine.model.RuleSet;

import java.io.IOException;
import java.util.List;

/**
 * Interface for loading rules from various sources
 */
public interface RuleLoader {
    
    /**
     * Load rules from a single source
     */
    RuleSet loadRules(String source) throws IOException;
    
    /**
     * Load rules from multiple sources
     */
    default List<RuleSet> loadRules(List<String> sources) throws IOException {
        return sources.stream()
            .map(source -> {
                try {
                    return loadRules(source);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load rules from: " + source, e);
                }
            })
            .toList();
    }
    
    /**
     * Check if this loader supports the given source
     */
    boolean supports(String source);
}

// src/main/java/com/example/ruleengine/loader/JsonRuleLoader.java
package com.example.ruleengine.loader;

import com.example.ruleengine.model.RuleSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Loads rules from JSON files
 */
@Component
public class JsonRuleLoader implements RuleLoader {
    
    private static final Logger log = LoggerFactory.getLogger(JsonRuleLoader.class);
    
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    
    public JsonRuleLoader(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }
    
    @Override
    public RuleSet loadRules(String source) throws IOException {
        log.info("Loading JSON rules from: {}", source);
        
        Resource resource = resourceLoader.getResource(source);
        if (!resource.exists()) {
            throw new IOException("Rule file not found: " + source);
        }
        
        try (var inputStream = resource.getInputStream()) {
            RuleSet ruleSet = objectMapper.readValue(inputStream, RuleSet.class);
            log.info("Successfully loaded {} rules from JSON file: {}", 
                ruleSet.rules().size(), source);
            return ruleSet;
        } catch (IOException e) {
            log.error("Failed to parse JSON rules from: {}", source, e);
            throw e;
        }
    }
    
    @Override
    public boolean supports(String source) {
        return source.toLowerCase().endsWith(".json");
    }
}

// src/main/java/com/example/ruleengine/loader/XmlRuleLoader.java
package com.example.ruleengine.loader;

import com.example.ruleengine.model.RuleSet;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Loads rules from XML files
 */
@Component
public class XmlRuleLoader implements RuleLoader {
    
    private static final Logger log = LoggerFactory.getLogger(XmlRuleLoader.class);
    
    private final XmlMapper xmlMapper;
    private final ResourceLoader resourceLoader;
    
    public XmlRuleLoader(ResourceLoader resourceLoader) {
        this.xmlMapper = new XmlMapper();
        this.resourceLoader = resourceLoader;
    }
    
    @Override
    public RuleSet loadRules(String source) throws IOException {
        log.info("Loading XML rules from: {}", source);
        
        Resource resource = resourceLoader.getResource(source);
        if (!resource.exists()) {
            throw new IOException("Rule file not found: " + source);
        }
        
        try (var inputStream = resource.getInputStream()) {
            RuleSet ruleSet = xmlMapper.readValue(inputStream, RuleSet.class);
            log.info("Successfully loaded {} rules from XML file: {}", 
                ruleSet.rules().size(), source);
            return ruleSet;
        } catch (IOException e) {
            log.error("Failed to parse XML rules from: {}", source, e);
            throw e;
        }
    }
    
    @Override
    public boolean supports(String source) {
        return source.toLowerCase().endsWith(".xml");
    }
}

// src/main/java/com/example/ruleengine/loader/TextRuleLoader.java
package com.example.ruleengine.loader;

import com.example.ruleengine.model.RuleSet;
import com.example.ruleengine.model.SpelRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads rules from plain text files where each line is a SpEL expression
 */
@Component
public class TextRuleLoader implements RuleLoader {
    
    private static final Logger log = LoggerFactory.getLogger(TextRuleLoader.class);
    
    private final ResourceLoader resourceLoader;
    
    public TextRuleLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @Override
    public RuleSet loadRules(String source) throws IOException {
        log.info("Loading text rules from: {}", source);
        
        Resource resource = resourceLoader.getResource(source);
        if (!resource.exists()) {
            throw new IOException("Rule file not found: " + source);
        }
        
        List<SpelRule> rules = new ArrayList<>();
        int lineNumber = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                
                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    continue;
                }
                
                try {
                    SpelRule rule = new SpelRule(
                        "rule_" + lineNumber,
                        "SpEL rule from line " + lineNumber,
                        lineNumber,
                        true,
                        trimmedLine
                    );
                    rules.add(rule);
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping invalid rule at line {}: {}", lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to read text rules from: {}", source, e);
            throw e;
        }
        
        log.info("Successfully loaded {} rules from text file: {}", rules.size(), source);
        return new RuleSet("TextRules", "1.0", List.copyOf(rules));
    }
    
    @Override
    public boolean supports(String source) {
        return source.toLowerCase().endsWith(".txt") || 
               source.toLowerCase().endsWith(".rules");
    }
}

// src/main/java/com/example/ruleengine/evaluator/RuleEvaluator.java
package com.example.ruleengine.evaluator;

import com.example.ruleengine.model.Rule;
import com.example.ruleengine.model.RuleEvaluationResult;

import java.util.Map;

/**
 * Interface for evaluating hierarchical rules with SpEL
 */
public interface RuleEvaluator {
    
    /**
     * Evaluate a rule against the provided data
     */
    RuleEvaluationResult evaluate(Rule rule, Object data);
    
    /**
     * Evaluate a rule against the provided data map (backward compatibility)
     */
    default RuleEvaluationResult evaluate(Rule rule, Map<String, Object> data) {
        return evaluate(rule, (Object) data);
    }
}

// src/main/java/com/example/ruleengine/evaluator/HierarchicalSpelRuleEvaluator.java
package com.example.ruleengine.evaluator;

import com.example.ruleengine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates hierarchical rules with nested condition groups using SpEL
 */
@Component
public class HierarchicalSpelRuleEvaluator implements RuleEvaluator {
    
    private static final Logger log = LoggerFactory.getLogger(HierarchicalSpelRuleEvaluator.class);
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    
    @Override
    public RuleEvaluationResult evaluate(Rule rule, Object data) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Evaluating rule: {} with data type: {}", rule.id(), data.getClass().getSimpleName());
            
            EvaluationContext context = createEvaluationContext(data);
            boolean result = evaluateConditionGroup(rule.rootConditionGroup(), context);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (result) {
                log.debug("Rule '{}' matched successfully", rule.name());
                return RuleEvaluationResult.success(rule.name(), executionTime);
            } else {
                String reason = String.format("Rule '%s' conditions not met", rule.name());
                return RuleEvaluationResult.failure(rule.name(), reason, executionTime);
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Error evaluating rule '{}': {}", rule.name(), e.getMessage(), e);
            return RuleEvaluationResult.failure(rule.name(), 
                "Evaluation error: " + e.getMessage(), executionTime);
        }
    }
    
    /**
     * Create evaluation context for SpEL expressions with nested object support
     */
    private EvaluationContext createEvaluationContext(Object data) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // Set root object for direct property access (e.g., customer.name)
        context.setRootObject(data);
        
        // If data is a Map, also set variables for #variable access
        if (data instanceof Map<?, ?> dataMap) {
            dataMap.forEach((key, value) -> {
                if (key instanceof String stringKey) {
                    context.setVariable(stringKey, value);
                }
            });
        }
        
        // Add utility functions and constants
        context.setVariable("empty", "");
        context.setVariable("null", null);
        
        return context;
    }
    
    /**
     * Evaluate a condition group with logical operators
     */
    private boolean evaluateConditionGroup(ConditionGroup group, EvaluationContext context) {
        log.debug("Evaluating condition group with operator: {}", group.logicalOperator());
        
        return switch (group.logicalOperator()) {
            case AND -> group.components().stream()
                .allMatch(component -> evaluateConditionComponent(component, context));
            case OR -> group.components().stream()
                .anyMatch(component -> evaluateConditionComponent(component, context));
        };
    }
    
    /**
     * Evaluate a single condition component (either condition or nested group)
     */
    private boolean evaluateConditionComponent(ConditionComponent component, EvaluationContext context) {
        if (component.isCondition()) {
            return evaluateCondition(component.condition(), context);
        } else if (component.isConditionGroup()) {
            return evaluateConditionGroup(component.conditionGroup(), context);
        } else {
            throw new IllegalStateException("Invalid condition component state");
        }
    }
    
    /**
     * Evaluate a single SpEL condition
     */
    private boolean evaluateCondition(Condition condition, EvaluationContext context) {
        try {
            String expr = condition.expression();
            log.debug("Evaluating SpEL expression: {}", expr);
            
            Expression expression = expressionCache.computeIfAbsent(expr, parser::parseExpression);
            Boolean result = expression.getValue(context, Boolean.class);
            
            log.debug("Expression '{}' evaluated to: {}", expr, result);
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            log.warn("Failed to evaluate condition '{}': {}", condition.expression(), e.getMessage());
            return false;
        }
    }
}

// src/main/java/com/example/ruleengine/service/RuleEngineService.java
package com.example.ruleengine.service;

import com.example.ruleengine.evaluator.RuleEvaluator;
import com.example.ruleengine.loader.RuleLoader;
import com.example.ruleengine.model.Rule;
import com.example.ruleengine.model.RuleEvaluationResult;
import com.example.ruleengine.model.RuleSet;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main service for rule engine operations
 */
@Service
public class RuleEngineService {
    
    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);
    
    private final List<RuleLoader> ruleLoaders;
    private final List<RuleEvaluator> ruleEvaluators;
    private final Map<String, List<Rule>> loadedRules = new ConcurrentHashMap<>();
    
    public RuleEngineService(List<RuleLoader> ruleLoaders, List<RuleEvaluator> ruleEvaluators) {
        this.ruleLoaders = ruleLoaders;
        this.ruleEvaluators = ruleEvaluators;
    }
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing Rule Engine Service with {} loaders and {} evaluators", 
            ruleLoaders.size(), ruleEvaluators.size());
    }
    
    /**
     * Load rules from a source file
     */
    public void loadRules(String source) throws IOException {
        log.info("Loading rules from source: {}", source);
        
        RuleLoader loader = ruleLoaders.stream()
            .filter(l -> l.supports(source))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No loader found for source: " + source));
        
        RuleSet ruleSet = loader.loadRules(source);
        loadedRules.put(source, ruleSet.rules());
        
        log.info("Successfully loaded {} rules from {}", ruleSet.rules().size(), source);
    }
    
    /**
     * Load rules from multiple sources
     */
    public void loadRules(List<String> sources) {
        sources.forEach(source -> {
            try {
                loadRules(source);
            } catch (IOException e) {
                log.error("Failed to load rules from {}: {}", source, e.getMessage());
            }
        });
    }
    
    /**
     * Evaluate a single rule against data
     */
    public RuleEvaluationResult evaluateRule(Rule rule, Map<String, Object> data) {
        if (!rule.isEnabled()) {
            return RuleEvaluationResult.failure(rule.name(), "Rule is disabled", 0);
        }
        
        RuleEvaluator evaluator = ruleEvaluators.stream()
            .filter(e -> e.supports(rule))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No evaluator found for rule type: " + rule.getClass().getSimpleName()));
        
        return evaluator.evaluate(rule, data);
    }
    
    /**
     * Evaluate all loaded rules against data
     */
    public List<RuleEvaluationResult> evaluateAllRules(Map<String, Object> data) {
        return getAllRules().stream()
            .filter(Rule::isEnabled)
            .sorted(Comparator.comparingInt(Rule::priority))
            .map(rule -> evaluateRule(rule, data))
            .toList();
    }
    
    /**
     * Evaluate rules from a specific source
     */
    public List<RuleEvaluationResult> evaluateRulesFromSource(String source, Map<String, Object> data) {
        List<Rule> rules = loadedRules.get(source);
        if (rules == null || rules.isEmpty()) {
            log.warn("No rules found for source: {}", source);
            return List.of();
        }
        
        return rules.stream()
            .filter(Rule::isEnabled)
            .sorted(Comparator.comparingInt(Rule::priority))
            .map(rule -> evaluateRule(rule, data))
            .toList();
    }
    
    /**
     * Get all loaded rules
     */
    public List<Rule> getAllRules() {
        return loadedRules.values().stream()
            .flatMap(List::stream)
            .toList();
    }
    
    /**
     * Get rules from a specific source
     */
    public List<Rule> getRulesFromSource(String source) {
        return loadedRules.getOrDefault(source, List.of());
    }
    
    /**
     * Clear all loaded rules
     */
    public void clearRules() {
        loadedRules.clear();
        log.info("All rules cleared");
    }
    
    /**
     * Get the first matching rule result
     */
    public RuleEvaluationResult getFirstMatch(Map<String, Object> data) {
        return getAllRules().stream()
            .filter(Rule::isEnabled)
            .sorted(Comparator.comparingInt(Rule::priority))
            .map(rule -> evaluateRule(rule, data))
            .filter(RuleEvaluationResult::matched)
            .findFirst()
            .orElse(new RuleEvaluationResult("NO_MATCH", false, "No rules matched", 
                java.time.LocalDateTime.now(), 0));
    }
}

// src/main/java/com/example/ruleengine/controller/RuleEngineController.java
package com.example.ruleengine.controller;

import com.example.ruleengine.model.Rule;
import com.example.ruleengine.model.RuleEvaluationResult;
import com.example.ruleengine.service.RuleEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for rule engine operations
 */
@RestController
@RequestMapping("/api/rules")
public class RuleEngineController {
    
    private static final Logger log = LoggerFactory.getLogger(RuleEngineController.class);
    
    private final RuleEngineService ruleEngineService;
    
    public RuleEngineController(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }
    
    @PostMapping("/load")
    public ResponseEntity<String> loadRules(@RequestParam String source) {
        try {
            ruleEngineService.loadRules(source);
            return ResponseEntity.ok("Rules loaded successfully from: " + source);
        } catch (IOException e) {
            log.error("Failed to load rules from: {}", source, e);
            return ResponseEntity.badRequest()
                .body("Failed to load rules: " + e.getMessage());
        }
    }
    
    @PostMapping("/load-multiple")
    public ResponseEntity<String> loadMultipleRules(@RequestBody List<String> sources) {
        ruleEngineService.loadRules(sources);
        return ResponseEntity.ok("Attempted to load rules from " + sources.size() + " sources");
    }
    
    @PostMapping("/evaluate")
    public ResponseEntity<List<RuleEvaluationResult>> evaluateRules(
            @RequestBody Map<String, Object> data) {
        List<RuleEvaluationResult> results = ruleEngineService.evaluateAllRules(data);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/evaluate/{source}")
    public ResponseEntity<List<RuleEvaluationResult>> evaluateRulesFromSource(
            @PathVariable String source,
            @RequestBody Map<String, Object> data) {
        List<RuleEvaluationResult> results = ruleEngineService.evaluateRulesFromSource(source, data);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/evaluate/first-match")
    public ResponseEntity<RuleEvaluationResult> getFirstMatch(
            @RequestBody Map<String, Object> data) {
        RuleEvaluationResult result = ruleEngineService.getFirstMatch(data);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<Rule>> getAllRules() {
        List<Rule> rules = ruleEngineService.getAllRules();
        return ResponseEntity.ok(rules);
    }
    
    @GetMapping("/list/{source}")
    public ResponseEntity<List<Rule>> getRulesFromSource(@PathVariable String source) {
        List<Rule> rules = ruleEngineService.getRulesFromSource(source);
        return ResponseEntity.ok(rules);
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearRules() {
        ruleEngineService.clearRules();
        return ResponseEntity.ok("All rules cleared");
    }
}

// src/main/java/com/example/ruleengine/RuleEngineApplication.java
package com.example.ruleengine;

import com.example.ruleengine.config.RuleEngineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RuleEngineProperties.class)
public class RuleEngineApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RuleEngineApplication.class, args);
    }
}

// src/main/resources/application.yml
spring:
  application:
    name: rule-engine
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
      indent-output: true

rule-engine:
  rule-files:
    - "classpath:rules/sample-rules.json"
    - "classpath:rules/business-rules.xml"
    - "classpath:rules/spel-rules.txt"
  default-rules-path: "classpath:rules/"
  enable-caching: true
  cache-expiration-minutes: 30
  enable-metrics: true

logging:
  level:
    com.example.ruleengine: DEBUG
    org.springframework.expression: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

server:
  port: 8080

// src/main/resources/rules/sample-rules.json
{
  "name": "Sample Rules",
  "version": "1.0",
  "rules": [
    {
      "type": "simple",
      "name": "age_check",
      "description": "Check if user is adult",
      "priority": 1,
      "enabled": true,
      "field": "age",
      "operator": "gte",
      "value": 18
    },
    {
      "type": "simple",
      "name": "email_validation",
      "description": "Validate email format",
      "priority": 2,
      "enabled": true,
      "field": "email",
      "operator": "matches",
      "value": "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    },
    {
      "type": "composite",
      "name": "premium_user",
      "description": "Check if user qualifies for premium features",
      "priority": 3,
      "enabled": true,
      "operator": "AND",
      "rules": [
        {
          "type": "simple",
          "name": "age_requirement",
          "description": "Must be adult",
          "priority": 1,
          "enabled": true,
          "field": "age",
          "operator": "gte",
          "value": 21
        },
        {
          "type": "simple",
          "name": "income_requirement",
          "description": "Must have sufficient income",
          "priority": 2,
          "enabled": true,
          "field": "income",
          "operator": "gt",
          "value": 50000
        }
      ]
    },
    {
      "type": "spel",
      "name": "complex_eligibility",
      "description": "Complex eligibility using SpEL",
      "priority": 4,
      "enabled": true,
      "expression": "#age >= 25 && (#income > 75000 || #hasGoodCredit) && #country == 'US'"
    }
  ]
}

// src/main/resources/rules/business-rules.xml
<?xml version="1.0" encoding="UTF-8"?>
<RuleSet>
    <name>Business Rules</name>
    <version>2.0</version>
    <rules>
        <Rule type="simple">
            <name>credit_score_check</name>
            <description>Minimum credit score requirement</description>
            <priority>1</priority>
            <enabled>true</enabled>
            <field>creditScore</field>
            <operator>gte</operator>
            <value>650</value>
        </Rule>
        <Rule type="simple">
            <name>employment_status</name>
            <description>Must be employed</description>
            <priority>2</priority>
            <enabled>true</enabled>
            <field>employmentStatus</field>
            <operator>eq</operator>
            <value>EMPLOYED</value>
        </Rule>
        <Rule type="composite">
            <name>loan_approval</name>
            <description>Loan approval criteria</description>
            <priority>3</priority>
            <enabled>true</enabled>
            <operator>AND</operator>
            <rules>
                <Rule type="simple">
                    <name>debt_ratio</name>
                    <description>Debt to income ratio</description>
                    <priority>1</priority>
                    <enabled>true</enabled>
                    <field>debtRatio</field>
                    <operator>lte</operator>
                    <value>0.4</value>
                </Rule>
                <Rule type="simple">
                    <name>min_income</name>
                    <description>Minimum annual income</description>
                    <priority>2</priority>
                    <enabled>true</enabled>
                    <field>annualIncome</field>
                    <operator>gte</operator>
                    <value>40000</value>
                </Rule>
            </rules>
        </Rule>
    </rules>
</RuleSet>

// src/main/resources/rules/spel-rules.txt
# SpEL Rules - Each line is a complete boolean expression
# Lines starting with # are comments and will be ignored

#age >= 18 && #email != null && #email.length() > 0
#income > 30000 && (#age >= 21 && #age <= 65)
(#creditScore >= 700 && #employmentYears >= 2) || (#income > 100000)
#country == 'US' && #state != null && #state.length() == 2
#hasInsurance && #vehicleYear >= 2015 && #drivingRecord == 'CLEAN'
(#membershipLevel == 'GOLD' || #membershipLevel == 'PLATINUM') && #accountBalance > 1000

// src/test/java/com/example/ruleengine/RuleEngineServiceTest.java
package com.example.ruleengine;

import com.example.ruleengine.model.RuleEvaluationResult;
import com.example.ruleengine.service.RuleEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RuleEngineServiceTest {
    
    @Autowired
    private RuleEngineService ruleEngineService;
    
    @Test
    void testLoadAndEvaluateJsonRules() throws IOException {
        // Load rules
        ruleEngineService.loadRules("classpath:rules/sample-rules.json");
        
        // Test data
        Map<String, Object> data = Map.of(
            "age", 25,
            "email", "test@example.com",
            "income", 60000,
            "hasGoodCredit", true,
            "country", "US"
        );
        
        // Evaluate rules
        List<RuleEvaluationResult> results = ruleEngineService.evaluateAllRules(data);
        
        assertFalse(results.isEmpty());
        
        // Check specific rule results
        RuleEvaluationResult ageCheck = results.stream()
            .filter(r -> "age_check".equals(r.ruleName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(ageCheck);
        assertTrue(ageCheck.matched());
    }
    
    @Test
    void testSpelRuleEvaluation() throws IOException {
        ruleEngineService.loadRules("classpath:rules/spel-rules.txt");
        
        Map<String, Object> data = Map.of(
            "age", 30,
            "email", "user@test.com",
            "income", 75000,
            "creditScore", 750,
            "employmentYears", 5,
            "country", "US",
            "state", "CA",
            "hasInsurance", true,
            "vehicleYear", 2020,
            "drivingRecord", "CLEAN",
            "membershipLevel", "GOLD",
            "accountBalance", 5000
        );
        
        List<RuleEvaluationResult> results = ruleEngineService.evaluateAllRules(data);
        
        assertFalse(results.isEmpty());
        
        // At least some rules should match
        assertTrue(results.stream().anyMatch(RuleEvaluationResult::matched));
    }
    
    @Test
    void testFirstMatchEvaluation() throws IOException {
        ruleEngineService.loadRules("classpath:rules/sample-rules.json");
        
        Map<String, Object> data = Map.of(
            "age", 25,
            "email", "test@example.com"
        );
        
        RuleEvaluationResult firstMatch = ruleEngineService.getFirstMatch(data);
        
        assertNotNull(firstMatch);
        if (firstMatch.matched()) {
            assertNotNull(firstMatch.ruleName());
        }
    }
}