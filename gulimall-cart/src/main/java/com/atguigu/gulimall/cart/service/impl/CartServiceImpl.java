package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

  @Autowired
  private ProductFeignService productFeignService;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  private ThreadPoolExecutor executor;

  private final String CART_PREFIX = "gulimall:cart:";

  @Override
  public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
    BoundHashOperations<String, Object, Object> cartOps = getCartOps();
    // TODO: 查看当前是否与这个购物车，如果有就只修改当前商品的数量
    String res = (String) cartOps.get(skuId.toString());
    if (StringUtils.isEmpty(res)) {
      // 2. 添加新商品到购物车
      CartItem cartItem = new CartItem();
      // 购物车无此商品
      // 1. 远程查询当前要添加的商品信息
      CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
        R skuInfo = productFeignService.getSkuInfo(skuId);
        SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
        });
        cartItem.setCheck(true);
        cartItem.setCount(num);
        cartItem.setImage(data.getSkuDefaultImg());
        cartItem.setTitle(data.getSkuTitle());
        cartItem.setSkuId(skuId);
        cartItem.setPrice(data.getPrice());
      }, executor);

      // 2. 远程查询sku的组合信息
      CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
        List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
        cartItem.setSkuAttr(values);
      }, executor);

      // 全部完成放行
      CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get();

      String s = JSON.toJSONString(cartItem);
      cartOps.put(skuId.toString(), s);
      return cartItem;
    } else {

      // 购物车有此商品，修改数量
      CartItem cartItem = JSON.parseObject(res, CartItem.class);
      cartItem.setCount(cartItem.getCount() + num);
      cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));

      return cartItem;
    }
  }

  @Override
  public CartItem getCartItem(Long skuId) {
    BoundHashOperations<String, Object, Object> cartOps = getCartOps();
    String res = (String) cartOps.get(skuId.toString());
    CartItem cartItem = JSON.parseObject(res, CartItem.class);
    return cartItem;
  }

  @Override
  public Cart getCart() throws ExecutionException, InterruptedException {
    Cart cart = new Cart();
    UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
    if (userInfoTo.getUserId() != null) {
      // 1. 登录了
      String cartKey = CART_PREFIX + userInfoTo.getUserId();
      // 2. 如果临时购物车的数据还没有进行合并【合并购物车】
      String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
      List<CartItem> tempCartItem = getCartItems(tempCartKey);
      if (tempCartItem != null) {
        // 临时购物车有数据，需要合并
        for (CartItem item : tempCartItem) {
          addToCart(item.getSkuId(), item.getCount());
        }
        // 清除临时购物车的数据
        clearCart(tempCartKey);
      }
      // 3. 获取登录后的购物车的数据 【包含合并过来的临时购物车的数据，和登录后的购物车的数据】
      List<CartItem> cartItems = getCartItems(cartKey);
      cart.setItems(cartItems);
    } else {
      // 2. 没登录
      String cartKey = CART_PREFIX + userInfoTo.getUserKey();
      // 获取临时购物车的所有购物项
      List<CartItem> cartItems = getCartItems(cartKey);
      cart.setItems(cartItems);
    }
    return cart;
  }

  // 获取到我们要操作的购物车
  private BoundHashOperations<String, Object, Object> getCartOps() {
    UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
    // 1.
    String cartKey = "";
    if (userInfoTo.getUserId() != null) { // 用户登录了
      // gulimall:cart:用户ID
      cartKey = CART_PREFIX + userInfoTo.getUserId();
    } else { // 没有登录，使用临时cookie   user-key
      cartKey = CART_PREFIX + userInfoTo.getUserKey();
    }
    BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
    return operations;
  }

  private List<CartItem> getCartItems(String cartKey) {
    BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
    List<Object> values = hashOps.values();
    if (values != null && values.size() > 0) {
      List<CartItem> collect = values.stream().map(obj -> {
        String str = (String) obj;
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
      }).collect(Collectors.toList());

      return collect;
    }
    return null;
  }

  @Override
  public void clearCart(String cartKey) {
    redisTemplate.delete(cartKey);
  }

  @Override
  public void checkItem(Long skuId, Integer check) {
    BoundHashOperations<String, Object, Object> cartOps = getCartOps();
    CartItem cartItem = getCartItem(skuId);
    cartItem.setCheck(check == 1);
    String s = JSON.toJSONString(cartItem);
    cartOps.put(skuId.toString(), s);
  }

  @Override
  public void changeItemCount(Long skuId, Integer num) {
    CartItem cartItem = getCartItem(skuId);
    cartItem.setCount(num);

    BoundHashOperations<String, Object, Object> cartOps = getCartOps();
    cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
  }

  @Override
  public void deleteItem(Long skuId) {
    BoundHashOperations<String, Object, Object> cartOps = getCartOps();
    cartOps.delete(skuId.toString());
  }

  @Override
  public List<CartItem> getUserCartItems() {
    UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
    if (userInfoTo.getUserId() == null) {
      return null;
    } else {
      String cartKey = CART_PREFIX + userInfoTo.getUserId();
      List<CartItem> cartItems = getCartItems(cartKey);
      if (cartItems != null && cartItems.size() > 0) {

        // TODO: 获取所有被选中的购物项
//        List<CartItem> collect = cartItems.stream()
//            .filter(item -> item.getCheck())
//            .map(item -> {
//              BigDecimal price = productFeignService.getPrice(item.getSkuId());
//              // 更新为最新价格
//              item.setPrice(price);
//              return item;
//            })
//            .collect(Collectors.toList());
        // 批量查询价格
        List<Long> list = cartItems.stream().filter(item -> item.getCheck())
            .map(i -> i.getSkuId()).collect(Collectors.toList());

        Map<Long, BigDecimal> map = productFeignService.getSkuIdAndPrice(list);

        List<CartItem> collect = cartItems.stream().filter(item -> item.getCheck())
            .map(item -> {
          // 更新为最新价格
          item.setPrice(map.get(item.getSkuId()));
          return item;
        }).collect(Collectors.toList());

        return collect;
      }
      return null;
    }
  }
}
