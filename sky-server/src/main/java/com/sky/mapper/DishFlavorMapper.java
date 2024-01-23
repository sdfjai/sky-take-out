package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /*
    * 批量插入口味数据
    **/
    void insertBatch(List<DishFlavor> flavors);

    //批量删除口味数据
    void deleteByIds(List<Long> ids);

    @Select("select * from dish_flavor where dish_id=#{dishId}")
    List<DishFlavor> getById(Long id);

    @Delete("delete from dish_flavor where dish_flavor.dish_id =#{dishID}")
    void deleteById(Long id);
}
