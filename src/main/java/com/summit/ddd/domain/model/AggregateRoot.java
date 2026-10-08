package com.summit.ddd.domain.model;


import com.summit.ddd.domain.model.exception.BusinessException;
import lombok.Getter;

/**
 * 领域层（Domain）聚合根基类
 */
@Getter
public abstract class AggregateRoot {
    public void throwIf(boolean condition,String message){
        if(condition){
            throw new BusinessException(message);
        }
    }
}
