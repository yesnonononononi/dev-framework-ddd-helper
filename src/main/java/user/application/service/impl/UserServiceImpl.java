package user.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.summit.ddd.application.vo.PageResult;
import com.summit.ddd.application.vo.Result;
import user.application.command.UserCommand;
import user.application.service.UserService;
import user.application.vo.UserVO;
import user.domain.model.User;
import user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * User 应用层服务实现（生成骨架）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    @Override
    public Result<UserVO> findById(Long id) {
        if (id == null)
            return Result.error("id is null");
        Optional<User> model = repository.findById(id);
        return model.map(m -> Result.success(toVO(m))).orElseGet(() -> Result.error("id 对应数据不存在"));
    }

    @Override
    public Result<PageResult<UserVO>> findPage(Integer page, Integer pageSize) {
        int current = page == null ? 1 : Math.max(page, 1);
        int size = pageSize == null ? 10 : Math.max(pageSize, 1);
        IPage<User> pageResult = repository.queryByPage(current, size);
        PageResult<UserVO> result = new PageResult<>(pageResult.getCurrent(), pageResult.getSize(),
                pageResult.getTotal(), pageResult.getRecords().stream().map(this::toVO).toList());
        return Result.success(result);
    }

    @Override
    public Result<Void> add(UserCommand command) {
        if (command == null)
            return Result.error("command is null");
        User model = toModel(command);
        repository.save(model);
        return Result.success();
    }

    @Override
    public Result<Void> update(UserCommand command) {
        if (command == null || command.getId() == null)
            return Result.error("id is null");
        Optional<User> opt = repository.findById(command.getId());
        if (opt.isEmpty())
            return Result.error("id 对应数据不存在");
        User model = opt.get();
        BeanUtils.copyProperties(command, model);
        repository.updateById(model);
        return Result.success();
    }

    @Override
    public Result<Void> delById(Long id) {
        if (id == null)
            return Result.error("id is null");
        repository.findById(id).ifPresent(repository::delete);
        return Result.success();
    }

    private UserVO toVO(User model) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(model, vo);
        return vo;
    }

    private User toModel(UserCommand command) {
        User model = new User();
        BeanUtils.copyProperties(command, model);
        return model;
    }
}
