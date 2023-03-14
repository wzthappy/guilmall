package com.atguigu.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class ListValueConstraintValidator implements ConstraintValidator<ListValue, Integer> {
  private Set<Integer> set = new HashSet<>();

  @Override  // 初始化方法
  public void initialize(ListValue constraintAnnotation) {
    int[] vals = constraintAnnotation.vals();
    for (int val : vals) {
      set.add(val);
    }
  }

  /**
   * @param value 需要校验的值
   * @param context context in which the constraint is evaluated
   * @return
   */
  @Override  // 判断是否校验成功
  public boolean isValid(Integer value, ConstraintValidatorContext context) {
    return set.contains(value);
  }
}
