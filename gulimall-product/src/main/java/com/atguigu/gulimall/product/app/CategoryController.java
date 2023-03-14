package com.atguigu.gulimall.product.app;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.common.utils.R;


/**
 * 商品三级分类
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 16:34:26
 */
@RestController
@RequestMapping("product/category")
public class CategoryController {
  @Autowired
  private CategoryService categoryService;

  /**
   * 查出所有分类以及子分类，以树形结构组装起来
   */
  @RequestMapping("/list/tree")
  public R list() {
    List<CategoryEntity> entities = categoryService.listWithTree();

    return R.ok().put("data", entities);
  }


  /**
   * 信息
   */
  @RequestMapping("/info/{catId}")
  public R info(@PathVariable("catId") Long catId) {
    CategoryEntity category = categoryService.getById(catId);

    return R.ok().put("data", category);
  }

  /**
   * 保存
   */
  @RequestMapping("/save")
  public R save(@RequestBody CategoryEntity category) {
    categoryService.save(category);

    return R.ok();
  }

  /**
   * 修改
   */
  @RequestMapping("/update/sort")
  public R updateSort(@RequestBody CategoryEntity[] category) {
    categoryService.updateBatchById(Arrays.asList(category));
    return R.ok();
  }

  /**
   * 修改
   */
  @RequestMapping("/update")
  public R update(@RequestBody CategoryEntity category) {
    categoryService.updateCascade(category);
    return R.ok();
  }

  /**
   * 删除
   * @RequestBody： 获取请求体，必须发送POST请求
   * Spring
   */
  @RequestMapping("/delete")
  public R delete(@RequestBody Long[] catIds) {
    // 检查当前删除的菜单，是否被别的地方引用
//    categoryService.removeByIds(Arrays.asList(catIds));
    categoryService.removeMenuByIds(Arrays.asList(catIds));

    return R.ok();
  }

}
