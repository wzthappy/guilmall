package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {
  // 查询到的所有商品信息
  private List<SkuEsModel> products;
  /**
   * 以下是分页信息
   */
  private Long total;      // 总记录数
  private Integer pageNum; // 当前页码
  private Integer totalPages; // 总页码
  private List<Integer> pageNavs;

  private List<BrandVo> brands; // 当前查询到的结果，所有涉及到的品牌
  private List<CatalogVo> catalogs; // 当前查询到的结果，所有涉及到的分类
  private List<AttrVo> attrs; // 当前查询到的结果，所有涉及到的所有属性


  // ============ 以上是返回给页面的所有信息 ============

  // 面包屑导航数据
  private List<NavVo> navs = new ArrayList<>();
  private List<Long> attrIds = new ArrayList<>();

  @Data
  public static class NavVo {
    private String navName;
    private String navValue;
    private String link;
  }

  @Data
  public static class BrandVo {
    private Long brandId;  // 品牌Id
    private String brandName; // 品牌名称
    private String brandImg; // 品牌图
  }

  @Data
  public static class CatalogVo {
    private Long catalogId; // 分类id
    private String catalogName; // 分类名称
  }

  @Data
  public static class AttrVo {
    private Long attrId;  // 属性Id
    private String attrName; // 属性名称
    private List<String> attrValue; // 具体属性
  }
}
