package user.domain.model;

import com.summit.ddd.domain.model.AggregateRoot;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User 聚合根（生成骨架）
 * <p>聚合标识 id 由 {@link AggregateRoot} 基类承载，不在此重复声明。业务字段与领域方法按需在生成后补充，
 * 建议创建/状态变更收敛为静态工厂与领域方法。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends AggregateRoot<Long> {

}
