package user.api.request;

import lombok.Data;

/**
 * User 接口层入参（生成骨架）
 * <p>骨架仅承载聚合标识，业务字段按需在生成后补充。</p>
 */
@Data
public class UserRequest {
    private Long id;
}
