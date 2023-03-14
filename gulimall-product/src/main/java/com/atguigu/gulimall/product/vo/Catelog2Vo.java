package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catelog2Vo {
  private String catalog1Id; // 一级分类ID
  private String id;   // 二级分类ID
  private String name; // 二级分类名称
  private List<Catelog3Vo> catalog3List; // 三级分类

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Catelog3Vo {
    private String catalog2Id; // 二级分类ID
    private String id;   // 三级分类ID
    private String name;  // 三级分类名称
  }
}
