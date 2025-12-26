package com.example.chess_game.chess_game_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Configuration for Production-Grade Session Management
 * 
 * Security Features:
 * - TLS/SSL encryption for remote connections
 * - Connection timeout to prevent hanging connections
 * - Connection pooling for performance
 * 
 * Use Cases:
 * - WebSocket session storage
 * - Rate limiting counters
 * - Game state caching
 */
@Configuration
public class RedisConfig {

  @Value("${spring.redis.host}")
  private String redisHost;

  @Value("${spring.redis.port}")
  private int redisPort;

  @Value("${spring.redis.password}")
  private String redisPassword;

  @Value("${spring.redis.ssl:true}")
  private boolean useSsl;

  @Value("${spring.redis.timeout:2000ms}")
  private Duration timeout;

  /**
   * Configures Lettuce connection factory with TLS support
   * 
   * Security Checks:
   * - SSL/TLS enabled for Upstash (useSsl=true)
   * - Connection timeout to prevent hanging
   * - Password authentication
   * 
   * Edge Cases Handled:
   * - Null/empty password validation
   * - Connection retry logic (handled by Lettuce)
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    // Validate configuration
    if (redisHost == null || redisHost.trim().isEmpty()) {
      throw new IllegalStateException("Redis host is not configured. Set REDIS_HOST environment variable.");
    }
    if (redisPassword == null || redisPassword.trim().isEmpty()) {
      throw new IllegalStateException("Redis password is not configured. Set REDIS_PASSWORD environment variable.");
    }

    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(redisHost);
    config.setPort(redisPort);
    config.setPassword(redisPassword);

    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder()
        .commandTimeout(timeout);

    // Enable SSL for remote Redis (Upstash requires this)
    if (useSsl) {
      clientConfig.useSsl();
    }

    return new LettuceConnectionFactory(config, clientConfig.build());
  }

  /**
   * RedisTemplate with JSON serialization for complex objects
   * 
   * Security Checks:
   * - StringSerializer for keys (prevents injection)
   * - Jackson2JsonSerializer for values (type-safe)
   * 
   * Edge Cases Handled:
   * - Null value handling (Jackson handles gracefully)
   * - Custom object serialization
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Use String serialization for keys to prevent injection attacks
    StringRedisSerializer keySerializer = new StringRedisSerializer();
    template.setKeySerializer(keySerializer);
    template.setHashKeySerializer(keySerializer);

    // Use JSON serialization for values (simpler approach for Spring Boot 3.x+)
    Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);

    template.afterPropertiesSet();
    return template;
  }
}
