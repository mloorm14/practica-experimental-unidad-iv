package ec.edu.uteq.pfcbackend.dto;

import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        List<String> errors,
        Map<String, Object> meta
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, List.of(), Map.of());
    }

    public static <T> ApiResponse<T> success(T data, String message, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, message, List.of(), meta);
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(false, null, message, errors, Map.of());
    }
}
