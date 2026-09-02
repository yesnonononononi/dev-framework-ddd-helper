package user.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.summit.ddd.infrastructure.repository.AbstractRepository;
import user.domain.model.User;
import user.domain.repository.UserRepository;
import user.infrastructure.persistence.mapper.UserMapper;
import user.infrastructure.persistence.po.UserPO;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

/**
 * User 仓储实现（生成骨架）
 */
@Repository
public class UserRepositoryImpl extends AbstractRepository<User, UserPO, Long>
        implements UserRepository {

    private final UserMapper mapper;

    public UserRepositoryImpl(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected @NotNull BaseMapper<UserPO> mapper() {
        return this.mapper;
    }

    @Override
    protected User toModel(UserPO po) {
        User model = new User();
        BeanUtils.copyProperties(po, model);
        return model;
    }

    @Override
    protected UserPO toPO(User model) {
        UserPO po = new UserPO();
        BeanUtils.copyProperties(model, po);
        return po;
    }
}
