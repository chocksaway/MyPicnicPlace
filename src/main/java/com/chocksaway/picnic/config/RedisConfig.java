package com.chocksaway.picnic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.chocksaway.picnic.image.ImageMetadata;
import com.chocksaway.picnic.model.PicnicRecord;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ImageMetadata> imageMetadataRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ImageMetadata> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(ImageMetadata.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, PicnicRecord> picnicRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, PicnicRecord> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(PicnicRecord.class));
        template.afterPropertiesSet();
        return template;
    }
}
