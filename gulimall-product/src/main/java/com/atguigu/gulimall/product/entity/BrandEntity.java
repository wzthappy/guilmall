package com.atguigu.gulimall.product.entity;

import com.atguigu.common.valid.AddGroup;
import com.atguigu.common.valid.ListValue;
import com.atguigu.common.valid.UpdateGrop;
import com.atguigu.common.valid.UpdateStatusGroup;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

/**
 * 品牌
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * 品牌id
   */
  @Null(message = "新增不能指定id", groups = {AddGroup.class})
  @NotNull(message = "修改编写指定品牌id", groups = {UpdateGrop.class})
  @TableId
  private Long brandId;
  /**
   * 品牌名
   */
  @NotBlank(message = "品牌名必须提交", groups = {UpdateGrop.class, AddGroup.class})
  private String name;
  /**
   * 品牌logo地址
   */
  @NotBlank(groups = {AddGroup.class})
  @URL(message = "logo必须是一个合法的URL地址", groups = {AddGroup.class, UpdateGrop.class})
  private String logo;
  /**
   * 介绍
   */
  private String descript;
  /**
   * 显示状态[0-不显示；1-显示]
   */
  @NotNull(groups = {AddGroup.class, UpdateStatusGroup.class, UpdateGrop.class})
  @ListValue(vals = {0, 1}, groups = {AddGroup.class, UpdateStatusGroup.class})
  private Integer showStatus;
  /**
   * 检索首字母
   */
  @NotNull(groups = {AddGroup.class})
  @Pattern(regexp = "^[a-zA-Z]$", message = "检索首字母必须是一个字母", groups = {AddGroup.class, UpdateGrop.class})
  private String firstLetter;
  /**
   * 排序
   */
  @NotNull(groups = {AddGroup.class})
  @Min(value = 0, message = "排序必须大于大于0", groups = {AddGroup.class, UpdateGrop.class})
  private Integer sort;
}
