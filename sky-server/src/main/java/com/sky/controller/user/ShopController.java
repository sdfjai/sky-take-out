package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Api(tags = "店铺相关接口")
@RestController("adminShopController")
@RequestMapping("/user/shop")
@Slf4j
public class ShopController {

    public static final String KEY="SHOP_STATUS";
    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("查询店铺状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
        return Result.success(shopStatus);
    }
}
