package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-15 14:40:41
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
