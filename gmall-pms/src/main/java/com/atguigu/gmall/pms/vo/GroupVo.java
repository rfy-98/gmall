package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;
@Data
@ToString
public class GroupVo extends AttrGroupEntity {
    private List<AttrEntity> attrEntities;
}
