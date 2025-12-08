package com.framework.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiResponse {
    public static Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        if (data instanceof List) {
            response.put("count", ((List<?>) data).size());
            response.put("data", data);
        } else {
            response.put("data", data);
        }
        return response;
    }

    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return response;
    }
}
