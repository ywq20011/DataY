package com.gaoding.datay.platform;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppController {
 

    @GetMapping("/")
    public String index(){
        return "redirect:index.html";
    }

}
