package com.bookstore.bookstore_api.util.validate;

import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * 도메인 객체의 유효성 검증을 위한 클래스
 * 객체가 생성될 때 자동으로 유효성을 검증하고, 유효하지 않은 경우 예외를 발생시킨다.
 */
public abstract class SelfValidating<T> {

    private Validator validator;

    public SelfValidating() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @SuppressWarnings("unchecked")
    protected void validateSelf() {
        Set<ConstraintViolation<T>> violations = validator.validate((T) this);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

}
