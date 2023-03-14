package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//  private Map<String, Object> cache = new HashMap<>();
//    @Autowired
//    private CategoryDao categoryDao;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  private RedissonClient redisson;

  @Autowired
  private CategoryBrandRelationService categoryBrandRelationService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<CategoryEntity> page = this.page(
        new Query<CategoryEntity>().getPage(params),
        new QueryWrapper<CategoryEntity>()
    );

    return new PageUtils(page);
  }

  @Override
  public List<CategoryEntity> listWithTree() {
    // 1. 查出所有分类
    List<CategoryEntity> entities = baseMapper.selectList(null);

    // 2. 组装成父子的树形结构
    // 2.1)、找到所有的一级分类
    List<CategoryEntity> level1Menus = entities.stream()
        .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
        .map((menu) -> {
          menu.setChildren(getChildrens(menu, entities));
          return menu;
        }).sorted((menu1, menu2) -> {
          return menu1.getSort() - menu2.getSort();
        })
        .collect(Collectors.toList());


    return level1Menus;
  }

  @Override
  public void removeMenuByIds(List<Long> asList) {
    // TODO  // 1、检查当前删除的菜单，是否被别的地方引用

    // 逻辑删除
    baseMapper.deleteBatchIds(asList);
  }

  // [2, 25, 225]
  @Override
  public Long[] findCatelogPath(Long catelogId) {
    List<Long> paths = new ArrayList<>();
    findParentPath(catelogId, paths);

    return paths.toArray(new Long[paths.size()]);
  }

  /**
   * 级联更新所有关联的数据
   *
   * @param category
   */
  @Override
//  @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'") // 缓存失效模式，删除对应的redis的key
//  @Caching(evict = {   // 同时进行多种缓存操作
//      @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'"), // 缓存失效模式，删除对应的redis的key
//      @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
//  })
  @CacheEvict(value = {"category"}, allEntries = true) //  allEntries = true  默认删除对应分区的全部key
  @Transactional // 事务
  public void updateCascade(CategoryEntity category) {
    this.updateById(category);
    categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    // 同时修改缓存中的数据，或删除  下次自己在次查询
    // redis.del("catalogJson");
  }

  @Override
  /**
   *   @Cacheable(value = {"category"}, key = "#root.method.name")
   * 1. 每一个需要缓存的数据我们都要指定要放到那个名字的缓存。【缓存的分区(按照业务类型)】
   * 2. 代表当前方法的结果需要缓存。如果缓存中有，这个方法不调用。如果缓存中没有，会调用方法，最后将方法的结果放入缓存
   * 3. 默认行为
   *      1)、如果缓存中有，方法不用调用
   *      2)、key默认自动生成； 缓存的名字::SimpleKey [] (自主生成的key值)
   *      3)、缓存的value的值: 默认使用jdk序列化机制，将序列化后的数据村到redis中
   *      4)、默认ttl时间: -1
   *   自定义:
   *     1)、指定生成的缓存使用key:  key属性指定，接受一个SpEL表达式
   *     2)、指定缓存的数据的存活时间
   *     3)、将数据保存为json格式
   */
  @Cacheable(value = {"category"}, key = "#root.method.name")
  public List<CategoryEntity> getLevel1Categorys() {
    List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    return categoryEntities;
  }

  @Override
  @Cacheable(value = {"category"}, key = "#root.methodName", sync = true)
  public Map<String, List<Catelog2Vo>> getCatalogJson() {
    System.out.println("查询了数据库");
    List<CategoryEntity> selectList = baseMapper.selectList(null);

    // 1. 查出所有1级分类
    List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
    // 2. 封装数据
    Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
      // 1. 每一个的一级分类， 查到这个这个一级分类的二级分类
      List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
      // 2. 封装上面的结果
      List<Catelog2Vo> catelog2Vos = null;
      if (categoryEntities != null) {
        catelog2Vos = categoryEntities.stream().map(l2 -> {
          // 1. 找当前二级分类的三级分类封装成vo
          List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());

          List<Catelog2Vo.Catelog3Vo> collect = null;
          if (level3Catelog != null) {
            // 2. 封装成指定格式
            collect = level3Catelog.stream().map(l3 -> {
              return new Catelog2Vo
                  .Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
            }).collect(Collectors.toList());
          }

          Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), l2.getCatId().toString(),
              l2.getName(), collect);
          return catelog2Vo;
        }).collect(Collectors.toList());
      }
      return catelog2Vos;
    }));

    return parent_cid;
  }

  // TODO 产生堆外内存溢出: OutOfDirectMemoryError
  // 1)、 springboot2.0 以后默认使用lettuce作为操作redis的客户端。他使用netty进行网络通信。
  // 2)、 lettuce的bug导致netty堆外内存溢出； netty 如果没有指定堆外内存，默认使用自己设置的堆大小的 内存大小 -Xmx300m
  //         可以通过-Dio.netty.maxDirectMemory进行设置
  // 解决方案: 不能使用-Dio.netty.maxDirectMemory只去调大堆外内存
  //  1)、升级lettuce客户端     2)、切换使用jedis
//  @Override
  public Map<String, List<Catelog2Vo>> getCatalogJson2() {
    /**
     * 1. 空结果缓存: 解决缓存穿透
     * 2. 设置过期时间(加随机值); 解决缓存雪崩
     * 3. 加锁: 解决缓存击穿
     */

    // 给缓存中放json字符串，拿出的json字符串，还用逆转为能用的对象类型; 【序列号与反序列化】
    // 1. 加入缓存逻辑， 缓存中存的数据是json字符串
    // JSON是跨语言，跨平台兼容
    String catalogJSON = redisTemplate.opsForValue().get("catalogJson");

    if (StringUtils.isEmpty(catalogJSON)) {
      //  2. 缓存中没有这个数据, 查询数据库
      System.out.println("缓存不命中...查询数据库....");
      Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();
      return catalogJsonFromDb;
    }
    System.out.println("缓存命中....直接返回...");
    // 转为我们指定的对象
    Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,
        new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    return result;
  }

  /**
   * 缓存里面的数据如何和数据库保存一致
   * 缓存数据库一致性
   * 1)、双写模式
   * 2)、失效模式
   *
   * @return
   */
  public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
    // 1. 锁的名字。锁的粒度，越细越快
    // 锁的粒度；具体缓存的是某个数据，11-号商品   product-11-lock     product-12-lock
    RLock lock = redisson.getLock("CatalogJson-lock");
    lock.lock();
    Map<String, List<Catelog2Vo>> dataFromDb;
    try {
      dataFromDb = getDataFromDb();
    } finally {
      lock.unlock();
    }
    return dataFromDb;
  }

  public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
    // 1. 占分布式锁。去redis占坑
    String uuid = UUID.randomUUID().toString();
    Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS); // 上锁
    if (lock) {
      System.out.println("获取分布式锁成功...");
      // 加锁成功... 执行业务
      Map<String, List<Catelog2Vo>> dataFromDb;
      try {
        dataFromDb = getDataFromDb();
      } finally {
        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
            "then\n" +
            "    return redis.call(\"del\",KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end";
        // 删除锁
        Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
            Arrays.asList("lock"), uuid);
      }

      // 获取值对比 + 对比成功删除  也要是原子操作  lua脚本解锁
//      String lockValue = redisTemplate.opsForValue().get("lock");
//      if (uuid.equals(lockValue)) {
//        // 删除我自己的锁
//        redisTemplate.delete("lock"); // 释放锁
//      }

      return dataFromDb;
    } else {
      // 加锁失败...  重试。synchronized()
      // 休眠100ms重试
      System.out.println("获取分布式锁失败...等待重试");
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return getCatalogJsonFromDbWithLocalLock(); // 自旋锁的方式
    }
  }

  private Map<String, List<Catelog2Vo>> getDataFromDb() {
    String catalogJSON = redisTemplate.opsForValue().get("catalogJson");
    if (!StringUtils.isEmpty(catalogJSON)) {
      // 缓存不为null直接返回
      Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,
          new TypeReference<Map<String, List<Catelog2Vo>>>() {
          });
      return result;
    }
    System.out.println("查询了数据库...");

    List<CategoryEntity> selectList = baseMapper.selectList(null);

    // 1. 查出所有1级分类
    List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
    // 2. 封装数据
    Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
      // 1. 每一个的一级分类， 查到这个这个一级分类的二级分类
      List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
      // 2. 封装上面的结果
      List<Catelog2Vo> catelog2Vos = null;
      if (categoryEntities != null) {
        catelog2Vos = categoryEntities.stream().map(l2 -> {
          // 1. 找当前二级分类的三级分类封装成vo
          List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());

          List<Catelog2Vo.Catelog3Vo> collect = null;
          if (level3Catelog != null) {
            // 2. 封装成指定格式
            collect = level3Catelog.stream().map(l3 -> {
              return new Catelog2Vo
                  .Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
            }).collect(Collectors.toList());
          }

          Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), l2.getCatId().toString(),
              l2.getName(), collect);
          return catelog2Vo;
        }).collect(Collectors.toList());
      }
      return catelog2Vos;
    }));

    // 3. 查到的数据在放入缓存，将对象转为json放在缓存中
    String s = JSON.toJSONString(parent_cid);        // 过期时间1天
    redisTemplate.opsForValue().set("catalogJson", s, 1, TimeUnit.DAYS);
    return parent_cid;
  }

  // 从数据库查询并封装分类数据
  public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {
//    // 1. 如果缓存中有就用缓存的
//    Map<String, List<Catelog2Vo>> catalogJson = (Map<String, List<Catelog2Vo>>) cache.get("getCatalogJson");
//    if (catalogJson == null) {
//      // 调用业务    返回数据放入缓存
//      cache.put("catalogJson", parent_cid);
//    }
//    return catalogJson;
    // 只要是同一把锁，就能锁住需要这个锁的所有线程
    // 1. synchronized(this); SprinBoot所有的组件在容器中都是单例的。  所有可以锁住
    // TODO 本地锁: synchronized, JUC(Lock)，在分布式情况下，需要所有，必须使用分布式锁
    synchronized (this) {
      return getDataFromDb();
    }
  }

  private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
    List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid)
        .collect(Collectors.toList());
    return collect;
  }

  // [2, 25, 225]
  private void findParentPath(Long catelogId, List<Long> paths) {
    CategoryEntity byId = this.getById(catelogId);

    if (byId.getParentCid() != 0) {
      findParentPath(byId.getParentCid(), paths);
    }

    // 1. 收集当前节点id
    paths.add(catelogId);
//    return paths;
  }

  // 递归查找所有菜单的子菜单
  private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
    List<CategoryEntity> chidren = all.stream().filter(categoryEntity -> {
      return Objects.equals(categoryEntity.getParentCid(), root.getCatId());
    }).map(categoryEntity -> {
      // 1、找到子菜单
      categoryEntity.setChildren(getChildrens(categoryEntity, all));
      return categoryEntity;
    }).sorted((menu1, menu2) -> {
      // 2、菜单的排序
      return (menu1.getSort() == null ? 0 : menu1.getSort()) -
          (menu2.getSort() == null ? 0 : menu2.getSort());
    }).collect(Collectors.toList());

    return chidren;
  }
}