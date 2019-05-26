package com.bpf.mvc.controller;

import com.bpf.mvc.annotation.Controller;
import com.bpf.mvc.annotation.RequestMapping;

@Controller
public class HelloController {

    @RequestMapping("hello.htm")
    public void hello() {
        System.out.println("hello");
    }

    @RequestMapping("world.htm")
    public void world() {
        System.out.println("world");
    }
}
