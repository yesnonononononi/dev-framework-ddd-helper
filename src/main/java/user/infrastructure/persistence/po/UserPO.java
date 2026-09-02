package user.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * User 持久化对象（生成骨架）
 * <p>骨架仅承载聚合标识，业务字段（含乐观锁版本号等持久化关注点）按需在生成后补充。</p>
 */
@Data
@TableName("user")
public class UserPO {
    private Long id;
}
