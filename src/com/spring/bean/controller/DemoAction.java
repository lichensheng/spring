package com.spring.bean.controller;


import com.spring.bean.service.DemoService;
import com.spring.bean.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by
 *
 * @author Lichensheng
 * @date 2018/7/13 23:14
 * @return
 */
@Controller(value = "demoAction")
@RequestMapping("/web")
public class DemoAction {
    @Autowired
    private DemoService demoService;
    @RequestMapping("/edit.json")
    public void edit(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") Integer id){
        String result = demoService.get(id);
        try{
            response.getWriter().write(result);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
