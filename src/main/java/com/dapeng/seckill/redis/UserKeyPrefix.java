package com.dapeng.seckill.redis;

public class UserKeyPrefix extends BaseKeyPrefix{

    public UserKeyPrefix(String prefix) {
        super(prefix);
    }
    public static UserKeyPrefix userKeyPrefixById = new UserKeyPrefix("id");
    public static UserKeyPrefix userKeyPrefixByName = new UserKeyPrefix("name");
}