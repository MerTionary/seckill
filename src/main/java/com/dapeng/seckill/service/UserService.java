package com.dapeng.seckill.service;

import com.dapeng.seckill.bean.User;
import com.dapeng.seckill.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserDao userDao;

    public User getUserById(Integer id){
        return userDao.getUserById(id);
    }

    // @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public boolean tx(){
        User u1 = new User();
        u1.setId(2);
        u1.setName("2222");
        userDao.insertUser(u1);

        User u2= new User();
        u2.setId(1);
        u2.setName("11111");
        userDao.insertUser(u2);
        return true;
    }

}