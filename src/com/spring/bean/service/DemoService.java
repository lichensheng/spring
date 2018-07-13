package com.spring.bean.service;

import com.spring.bean.annotation.Service;

/**
 * Created by
 *
 * @author Lichensheng
 * @date 2018/7/13 23:24
 * @return
 */
@Service
public class DemoService {
    public String get(int id){
        return "Hello,World " + id;
    }

}
