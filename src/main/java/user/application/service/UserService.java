package user.application.service;

import com.summit.ddd.application.vo.PageResult;
import com.summit.ddd.application.vo.Result;
import user.application.command.UserCommand;
import user.application.vo.UserVO;

/**
 * User 应用层服务接口（生成骨架）
 */
public interface UserService {
    Result<UserVO> findById(Long id);

    Result<PageResult<UserVO>> findPage(Integer page, Integer pageSize);

    Result<Void> add(UserCommand command);

    Result<Void> update(UserCommand command);

    Result<Void> delById(Long id);
}
