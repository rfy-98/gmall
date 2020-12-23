package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.vo.PmsSkuSaleVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;

/**
 * 商品spu积分设置
 *
 * @author yige
 * @email 1020972669@qq.com
 * @date 2020-12-14 21:20:37
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    void saveSkuSaleInfo(PmsSkuSaleVo skuSaleVo);
}

