package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetMealService {
    SetmealVO getById(Long id);

    void insert(SetmealDTO setmealDTO);

    void startOrStop(Integer status, Long id);

    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    void update(SetmealDTO setmealDTO);

    void deleteBatch(List<Long> ids);
}
