package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

  @Autowired
  private RestHighLevelClient client;

  @Autowired
  private ProductFeignService productFeignService;

  // 去es进行检索
  @Override
  public SearchResult search(SearchParam param) {
    // 动态构建出查询需要的DSL语句
    SearchResult result = null;

    // 1. 准备检索请求
    SearchRequest request = buildSearchRequrest(param);

    try {
      // 2. 发送请求，等到响应
      SearchResponse response = client.search(request, GulimallElasticSearchConfig.COMMON_OPTIONS);

      // 3. 分析响应数据，封装成我们需要1的格式
      result = buildSearchResult(response, param);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * @return 准备检索请求
   */
  private SearchRequest buildSearchRequrest(SearchParam param) {
    // 1. 指定索引
    SearchRequest request = new SearchRequest(EsConstant.PRODUCT_INDEX);

    // 2. 组织DSL参数
    SearchSourceBuilder sourceBuilder = request.source();

    /**
     * 模糊匹配、过滤 (按照属性、分类、品牌、价格区间、库存)
     */
    // 1. 构建bool - query
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    /// 1.1 must
    if (!StringUtils.isEmpty(param.getKeyword())) {
      boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
    }

    // 1.2 bool -> filter     按照三级分类Id查询      (filter不参与算法，且查询效率快一点)
    if (param.getCatalog3Id() != null) {
      boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
    }

    // 1.2 bool -> filter     按照品牌Id查询
    if (param.getBrandId() != null && param.getBrandId().size() > 0) {
      boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
    }

    // 1.2 bool -> filter     按照所有指定的属性进行查询   attrs=1_安卓:苹果 & attrs=2_5寸:6寸
    if (param.getAttrs() != null && param.getAttrs().size() > 0) {
      for (String attrStr : param.getAttrs()) {
        BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
        // attrs=1_安卓:苹果...
        String[] s = attrStr.split("_");
        String attrId = s[0]; // 检索的属性Id
        String[] attrValues = s[1].split(":"); // 这个属性的检索用的值
        nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
        nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
        // 每一个必须
        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
        boolQuery.filter(nestedQuery);
      }
    }

    // 1.2 bool -> filter     按照库存是否有进行查询   hasStock=0/1   // 是否有货
    if (param.getHasStock() != null) {
      boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
    }

    // 1.2 bool -> filter     按照价格区间查询    skuPrice=1_500/_500/500_ // 价格区间
    if (!StringUtils.isEmpty(param.getSkuPrice())) {
      RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
      String[] s = param.getSkuPrice().split("_"); // skuPrice=1_500/_500/500
      if (s.length == 2) {
        // 区间
        rangeQuery.gte("".equals(s[0]) ? 0 : s[0]).lte(s[1]);
      } else if (s.length == 1) {
        rangeQuery.gte(s[0]);
      }
      boolQuery.filter(rangeQuery);
    }

    // 把以前的所有条件都拿出来进行封装
    sourceBuilder.query(boolQuery);

    /**
     * 排序、分页、高亮
     */
    // 2.1 排序  sort=saleCount_asc/desc  // 按 销量排序
    if (!StringUtils.isEmpty(param.getSort())) {
      String[] s = param.getSort().split("_");
      sourceBuilder.sort(s[0], "asc".equalsIgnoreCase(s[1]) ? SortOrder.ASC : SortOrder.DESC);
    }

    // 2.2 分页
    sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE).size(EsConstant.PRODUCT_PAGESIZE);

    // 2.3 高亮
    if (!StringUtils.isEmpty(param.getKeyword())) {
      sourceBuilder.highlighter(new HighlightBuilder()
          .field("skuTitle").preTags("<b style='color: red'>").postTags("</b>"));
    }

    /**
     * 聚合分析
     */
    // 3.1 品牌聚合
    TermsAggregationBuilder brandAgg =
        AggregationBuilders.terms("brand_agg").field("brandId").size(50);
    // 品牌聚合子聚合
    brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
    brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
    // TODO 1. 聚合brand
    sourceBuilder.aggregation(brandAgg);

    // 3.2 分类聚合
    TermsAggregationBuilder catalogAgg =
        AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
    catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));

    // TODO 2. 聚合catalog
    sourceBuilder.aggregation(catalogAgg);

    // 3.3 属性聚合
    NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
    // 聚合出当前attrId
    TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
    // 聚合分析出当前attrId对应的名字
    attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
    // 聚合分析出当前attrId对应的所有可能的属性值attrValue
    attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
    attrAgg.subAggregation(attrIdAgg);
    // TODO 3. 聚合attr
    sourceBuilder.aggregation(attrAgg);

    System.out.println("构建的DSL" + sourceBuilder.toString());
    return request;
  }

  /**
   * @return 构建结果数据
   */
  private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
    SearchResult result = new SearchResult();
    SearchHits hits = response.getHits();
    // 1. 返回的所有查询到的商品
    List<SkuEsModel> esModels = new ArrayList<>();
    if (hits.getHits() != null && hits.getHits().length > 0) {
      for (SearchHit hit : hits.getHits()) {
        SkuEsModel esModel = JSON.parseObject(hit.getSourceAsString(), new TypeReference<SkuEsModel>() {
        });
        if (!StringUtils.isEmpty(param.getKeyword())) {
          HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
          String string = skuTitle.fragments()[0].string();
          esModel.setSkuTitle(string);
        }
        esModels.add(esModel);
      }
    }
    result.setProducts(esModels);

    // 2. 当前所有商品涉及到的所有属性信息
    List<SearchResult.AttrVo> attrVos = new ArrayList<>();
    ParsedNested attrAgg = response.getAggregations().get("attr_agg");
    ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
    for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
      SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
      // 1. 得到属性的Id
      long attrId = bucket.getKeyAsNumber().longValue();
      // 2. 得到属性的名称
      ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
      String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
      // 3. 得到属性的所有值
      ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
      List<String> attrValues = new ArrayList<>();
      for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
        String attrValue = attrValueAggBucket.getKeyAsString();
        attrValues.add(attrValue);
      }
      attrVo.setAttrId(attrId);
      attrVo.setAttrName(attrName);
      attrVo.setAttrValue(attrValues);

      attrVos.add(attrVo);
    }
    result.setAttrs(attrVos);

    // 3. 当前所有商品涉及到的所有品牌信息
    ArrayList<SearchResult.BrandVo> brandVos = new ArrayList<>();
    ParsedLongTerms brandAgg = response.getAggregations().get("brand_agg");
    for (Terms.Bucket bucket : brandAgg.getBuckets()) {
      SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
      // 1. 得到品牌的id
      long brandId = bucket.getKeyAsNumber().longValue();
      // 2. 得到品牌的名
      ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
      String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
      // 3. 得到品牌的图片
      ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
      String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();

      brandVo.setBrandId(brandId);
      brandVo.setBrandName(brandName);
      brandVo.setBrandImg(brandImg);

      brandVos.add(brandVo);
    }
    result.setBrands(brandVos);

    // 4. 当前所有商品涉及到的所有分类信息
    ArrayList<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
    ParsedLongTerms catalogAgg = response.getAggregations().get("catalog_agg");
    for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
      SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
      // 得到分类Id
      String catalogId = bucket.getKeyAsString();
      catalogVo.setCatalogId(Long.parseLong(catalogId));

      // 得到分类名称
      ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
      String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
      catalogVo.setCatalogName(catalogName);

      catalogVos.add(catalogVo);
    }
    result.setCatalogs(catalogVos);

    // ======== 以上从聚合信息中获取 ========
    // 5. 分页信息 - 当前页码
    result.setPageNum(param.getPageNum());
    // 6. 分页信息 - 总记录数
    long total = hits.getTotalHits().value;
    result.setTotal(total);
    // 7. 分页信息 - 总页码 - 计算
    int totalPages = ((int) total + EsConstant.PRODUCT_PAGESIZE - 1) / EsConstant.PRODUCT_PAGESIZE;
    result.setTotalPages(totalPages);

    List<Integer> pageNavs = new ArrayList<>();
    for (int i = 1; i <= totalPages; i++) {
      pageNavs.add(i);
    }
    result.setPageNavs(pageNavs);

    // 6. 构建面包屑导航功能
    if (param.getAttrs() != null && param.getAttrs().size() > 0) {
      List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
        // 1. 分析每一个attrs传过来的查询参数值
        SearchResult.NavVo navVo = new SearchResult.NavVo();
        // attrs=2_5寸:6寸
        String[] s = attr.split("_");

        System.out.println(Arrays.toString(s));

        navVo.setNavValue(s[1]);
        R r = productFeignService.attrsInfo(Long.parseLong(s[0]));
        result.getAttrIds().add(Long.parseLong(s[0]));
        if (r.getCode() == 0) {
          AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
          });
          navVo.setNavName(data.getAttrName());
        } else {
          navVo.setNavName(s[0]);
        }
        // 2. 取消了这个面包屑以后，我们要跳转到那个地方，将请求地址的url里面的当前置空
        // 拿到所有的查询条件，去掉当前。
        String replace = replaceQueryString(param, attr, "attrs");
        navVo.setLink("http://search.gulimall.com/list.html?" + replace);
        return navVo;
      }).collect(Collectors.toList());

      result.setNavs(collect);
    }

    // 品牌，分类
    if (param.getBrandId() != null && param.getBrandId().size() > 0) {
      List<SearchResult.NavVo> navs = result.getNavs();
      SearchResult.NavVo navVo = new SearchResult.NavVo();
      navVo.setNavName("品牌");
      // TODO 远程查询所有品牌
      R r = productFeignService.brandsInfo(param.getBrandId());
      if (r.getCode() == 0) {
        List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>() {
        });
        StringBuffer buffer = new StringBuffer();
        String  replace = "";
        for (BrandVo brandVo : brand) {
          buffer.append(brandVo.getName() + ";");
          replace = replaceQueryString(param, brandVo.getBrandId() + "", "brandId");
        }
        navVo.setNavValue(buffer.toString());
        navVo.setLink("http://search.gulimall.com/list.html?" + replace);
      }

      navs.add(navVo);
    }

    // TODO 分类: 不需要导航取消

    return result;
  }

  private String replaceQueryString(SearchParam param, String value, String key) {
    String encode = null;
    try {
      encode = URLEncoder.encode(value, "UTF-8");
      encode = encode.replace("+", "%20"); // 浏览器对空格编码和java不一样
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    String replace = param.get_queryString().replace("&" + key + "=" + encode, "");
    return replace;
  }
}
