package user.application.command;

import lombok.Data;

/**
 * User 应用层命令（生成骨架）
 * <p>骨架仅承载聚合标识，业务字段按需在生成后补充。</p>
 */
@Data
public class UserCommand {
    private Long id;
}
