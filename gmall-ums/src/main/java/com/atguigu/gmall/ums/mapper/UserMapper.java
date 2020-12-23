package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author yige
 * @email 1020972669@qq.com
 * @date 2020-12-15 00:00:38
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
