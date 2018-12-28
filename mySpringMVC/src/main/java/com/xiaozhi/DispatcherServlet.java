package com.xiaozhi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xiaozhi.annotation.Controller;
import com.xiaozhi.annotation.Quatifier;
import com.xiaozhi.annotation.RequestMapping;
import com.xiaozhi.controller.SpringmvcController;

public class DispatcherServlet extends HttpServlet{
	private static final long serialVersionUID = 1L;

	List<String> packageNames = new ArrayList<String>();
	Map<String, Object> instanceMap = new HashMap<String, Object>();
	Map<String, Object> handleMap = new HashMap<String, Object>();

	public DispatcherServlet() {
		super();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		scanPackage("com.xiaozhi");
		try {
			filterAndInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 建立映射关系
		handleMap();
		// 实现注入
		ioc();
	}

	private void ioc() {
		if (instanceMap.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field: fields) {
				field.setAccessible(true);// 可访问私有属性
				if (field.isAnnotationPresent(Quatifier.class)) {
					Quatifier quatifier = field.getAnnotation(Quatifier.class);
					String value = quatifier.value();
					field.setAccessible(true);
					try {
						field.set(entry.getValue(), instanceMap.get(value));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		SpringmvcController spc = (SpringmvcController) instanceMap.get("xiaozhi");
		System.out.println(spc);
	}

	/**
	 * 过滤，实例化
	 * @throws Exception
	 */
	private void filterAndInstance() throws Exception {
		if (packageNames.size() <= 0) {
			return;
		}
		for (String className: packageNames) {
			Class<?> cName = Class.forName(className.replace(".class", ""));
			if (cName.isAnnotationPresent(Controller.class)) {
				Object instance = cName.newInstance();
				Controller controller = cName.getAnnotation(Controller.class);
				String key = controller.value();
				instanceMap.put(key, instance);
			} else {
				continue;
			}
		}
	}

	/**
	 * 建立映射关系
	 */
	private void handleMap() {
		if (instanceMap.size() <= 0) {
			return;
		}
		for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
			if (entry.getValue().getClass().isAnnotationPresent(Controller.class)) {
				Controller controller = entry.getValue().getClass().getAnnotation(Controller.class);
				String ctValue = controller.value();
				Method[] methods = entry.getValue().getClass().getMethods();
				for (Method method: methods) {
					if (method.isAnnotationPresent(RequestMapping.class)) {
						RequestMapping rm = method.getAnnotation(RequestMapping.class);
						String rmValue = rm.value();
						handleMap.put("/" + ctValue + "/" + rmValue, method);
					} else {
						continue;
					}
				}
			} else {
				continue;
			}
		}
	}

	/**
	 * 扫描包
	 * @param packagePath
	 */
	private void scanPackage(String packagePath) {
		// 将所有的.转义获取对应的路径
		URL url = this.getClass().getClassLoader().getResource("/" + replaceTo(packagePath));
		String filePath = url.getFile();
		File file = new File(filePath);
		String[] fileStrs = file.list();
		for (String fileStr: fileStrs) {
			File eachFile = new File(filePath + fileStr);
			if (eachFile.isDirectory()) {
				scanPackage(packagePath + "." + eachFile.getName());
			} else {
				packageNames.add(packagePath + "." + eachFile.getName());
			}
		}
	}

	private String replaceTo(String path) {
		return path.replaceAll("\\.", "/");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse rep) 
			throws ServletException, IOException{
		doPost(req, rep);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse rep) 
			throws ServletException, IOException {
		String url = req.getRequestURI();
		String context = req.getContextPath();
		String path = url.replace(context, "");
		Method method = (Method)handleMap.get(path);
		SpringmvcController controller = (SpringmvcController) instanceMap.get(path.split("/")[1]);
		try {
			method.invoke(controller, new Object[] {req, rep, null});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
