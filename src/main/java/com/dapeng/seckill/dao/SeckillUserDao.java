package com.dapeng.seckill.dao;

import com.dapeng.seckill.bean.SeckillUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface SeckillUserDao {

    @Select("select * from seckill_user where id=#{id}")
    public SeckillUser getSkillUserById(Long id);

    @Update("update set password=#{password} where id=#{id}")
    public Boolean updateUser(SeckillUser user);

}