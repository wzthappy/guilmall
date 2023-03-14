package com.atguigu.gulimall.search.vo;

import io.swagger.models.auth.In;
import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可以传递过来的程序条件
 *  keyword=32 & catalog3Id=165 & sort=saleCount_asc & hasStock=0/1
 *      & brandId=1 & brandId=2
 */
@Data
public class SearchParam {
  private String keyword; // 页面传递过来的全文匹配关键字

  private Long catalog3Id; // 三级分类Id
  /**
   * sort=saleCount_asc/desc  // 按 销量排序
   * sort=skuPrice_asc/desc   // 按 价格排序
   * sort=hotScore_asc/desc   // 按 热度评分排序
   */
  private String sort; // 排序条件
  /**
   *     过滤条件
   *  hasStock=0/1   // 是否有货
   *  skuPrice=1_500/_500/500_ // 价格区间
   *  brandId=1 & brandId=2   // 按照品牌Id进行查询，可以多选
   *  attrs=1_安卓:苹果 & attrs=2_5寸:6寸 // 属性进行筛选
   */
  private Integer hasStock; // 是否只显示有货的商品  0(无库存)   1(有库存)
  private String skuPrice;  // 价格区间查询
  private List<Long> brandId;  // 按照品牌Id进行查询，可以多选
  private List<String> attrs;  // 按照属性进行筛选
  private Integer pageNum = 1;  // 页码

  private String _queryString; // 原生的所有查询条件
}
