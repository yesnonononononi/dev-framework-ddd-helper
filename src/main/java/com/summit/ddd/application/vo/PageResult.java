package com.summit.ddd.application.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
@Builder
@Data
@AllArgsConstructor
public class PageResult<T> {
    private Long current;
    private Long pageSize;
    private Long total;
    private Collection<T> records;
}
