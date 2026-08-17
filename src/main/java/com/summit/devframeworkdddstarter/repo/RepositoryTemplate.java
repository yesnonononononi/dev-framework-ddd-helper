package com.summit.devframeworkdddstarter.repo;

import java.util.Collection;
import java.util.Optional;

public interface RepositoryTemplate<M,P> {

    void save(M entity);


    void delete(M entity);

    Optional<M> findById(Long id);

    void updateById(M entity);

    void update(Collection<M> list);
}
