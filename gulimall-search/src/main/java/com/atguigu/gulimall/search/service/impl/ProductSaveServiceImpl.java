package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

  @Autowired
  private RestHighLevelClient restHighLevelClient;

  @Override
  public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
    // 保存到es
    // 1. 在控制台中 给es中建立索引。product，建立好映射关系。
    // 2. 给es中保存这些数据
    BulkRequest request = new BulkRequest();
    for (SkuEsModel model : skuEsModels) {
      // 2.1 构造保存请求
      IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX)
          .id(model.getSkuId().toString())
          .source(JSON.toJSONString(model), XContentType.JSON);
      request.add(indexRequest);
    }

    BulkResponse bulk = restHighLevelClient.bulk(request, GulimallElasticSearchConfig.COMMON_OPTIONS);

    // TODO  判断是否有添加错误的文档
    boolean b = bulk.hasFailures();
    if (b) {
      List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
        return item.getId();
      }).collect(Collectors.toList());
      log.error("商城上架错误: {}", collect);
    }
    return b;
  }
}
