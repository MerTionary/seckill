package com.dapeng.seckill.dao;

import com.dapeng.seckill.bean.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface UserDao {

    @Select("select * from user where id = #{id}")
    public User getUserById(Integer id);

    @Insert("insert into user (id, name) values(#{id}, #{name})")
    public int insertUser(User user);
}