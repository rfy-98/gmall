package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author yige
 * @email 1020972669@qq.com
 * @date 2020-12-14 19:29:38
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    PageResultVo querySpu(PageParamVo pageParamVo, Long categoryId);

    void bigSave(SpuVo spu);

    void saveSku(SpuVo spuVo, Long spuId);

    void saveBaseAttr(SpuVo spuVo, Long spuId);

    void saveSpuDesc(SpuVo spuVo, Long spuId);

    Long saveSpu(SpuVo spuVo);
}

