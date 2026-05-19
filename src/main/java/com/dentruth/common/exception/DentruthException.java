package com.dentruth.common.exception;

import com.dentruth.common.response.code.ErrorStatus;
import lombok.Getter;

@Getter
public class DentruthException extends RuntimeException {

  private final ErrorStatus errorStatus;

  public DentruthException(ErrorStatus errorStatus) {
    super(errorStatus.getMessage());
    this.errorStatus = errorStatus;
  }

}
