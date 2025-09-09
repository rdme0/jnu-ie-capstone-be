package jnu.ie.capstone.rtzr.cache.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "AccessToken", timeToLive = 3600 * 6 - 60)
@Getter
@AllArgsConstructor
public class RtzrAccessToken {

    @Id
    private String id;
    private String value;
}