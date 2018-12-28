package com.xiaozhi.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xiaozhi.annotation.Controller;
import com.xiaozhi.annotation.RequestMapping;

@Controller("manage")
public class SpringmvcController {

	@RequestMapping("insert")
	public String insert(HttpServletRequest request, HttpServletResponse response, 
			String param) {
		return "OK";
	}
}
