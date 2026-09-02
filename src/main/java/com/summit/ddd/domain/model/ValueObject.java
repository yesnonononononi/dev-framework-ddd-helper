package com.summit.ddd.domain.model;

import lombok.Getter;

public class ValueObject<T> {
  @Getter
  private T value;


}
